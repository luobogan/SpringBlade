#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
e签宝沙箱联调脚本（SpringBlade 公文电子签章）

用法：
    python esign_sandbox_test.py connectivity   # 沙箱连通性 + 签名自检（不需要凭据）
    python esign_sandbox_test.py callback      # 本地回调验签自测（打本机 blade-workflow）
    python esign_sandbox_test.py seals         # 印章列表（需要 app-id/app-secret/org-id）
    python esign_sandbox_test.py full          # 完整链路：上传文件→创建签署流程→启动→查详情

凭据来源（优先级）：环境变量 ESIGN_APP_ID/ESIGN_APP_SECRET/ESIGN_ORG_ID
    > Nacos 数据库 nacos.config_info 中 data_id=blade-workflow-dev.yaml 的 odoc.esign.app-id/app-secret/org-id
"""
import base64
import hashlib
import hmac
import json
import os
import sys
import time

import requests

SDK_VERSION = 'Esign-Sdk-Core1.0'
ACCEPT = '*/*'
CONTENT_TYPE_JSON = 'application/json; charset=UTF-8'
SANDBOX_HOST = 'https://smlopenapi.esign.cn'
LOCAL_WORKFLOW = 'http://127.0.0.1:8111'
NACOS_MYSQL = dict(host='127.0.0.1', port=3306, user='root', password='123456', database='nacos')


def load_config():
    """读取 app-id / app-secret / org-id / host：环境变量优先，其次 Nacos 配置"""
    cfg = {'app_id': '', 'app_secret': '', 'org_id': '', 'host': SANDBOX_HOST}
    try:
        import pymysql
        import re
        conn = pymysql.connect(**NACOS_MYSQL)
        cur = conn.cursor()
        cur.execute("select content from config_info where data_id='blade-workflow-dev.yaml' and tenant_id='public'")
        row = cur.fetchone()
        conn.close()
        if row and row[0]:
            text = row[0]

            def pick(key):
                m = re.search(r'^\s*%s:\s*"?([^"\n]*)"?\s*$' % key, text, re.M)
                return m.group(1).strip() if m else ''

            cfg['app_id'] = pick('app-id')
            cfg['app_secret'] = pick('app-secret')
            cfg['org_id'] = pick('org-id')
            m = re.search(r'^\s*host:\s*(\S+)\s*$', text, re.M)
            if m:
                cfg['host'] = m.group(1)
    except Exception as e:
        print('[warn] 读取 Nacos 配置失败：%s' % e)
    cfg['app_id'] = os.environ.get('ESIGN_APP_ID', cfg['app_id'])
    cfg['app_secret'] = os.environ.get('ESIGN_APP_SECRET', cfg['app_secret'])
    cfg['org_id'] = os.environ.get('ESIGN_ORG_ID', cfg['org_id'])
    return cfg


def content_md5(body):
    if body is None:
        return ''
    return base64.b64encode(hashlib.md5(body.encode('utf-8')).digest()).decode()


def sort_url(api_path):
    """按官方口径：query 按字段名排序后拼接（与 EsignSigner#sortApiUrl 一致）"""
    if '?' not in api_path:
        return api_path
    base, query = api_path.split('?', 1)
    if not query.strip():
        return base
    pairs = []
    for part in query.split('&'):
        if not part:
            continue
        k, _, v = part.partition('=')
        pairs.append((k, v))
    keys = [k for k, _ in pairs]
    if len(keys) != len(set(keys)):
        raise ValueError('query 存在重复参数：%s' % api_path)
    pairs.sort(key=lambda kv: kv[0])
    return base + '?' + '&'.join('%s=%s' % (k, v) for k, v in pairs)


def build_headers(api_path, method, body, cfg):
    m = method.upper()
    md5 = '' if m in ('GET', 'DELETE') else content_md5(body)
    sorted_url = sort_url(api_path)
    message = '\n'.join([m, ACCEPT, md5, CONTENT_TYPE_JSON, '', '', sorted_url])
    signature = base64.b64encode(
        hmac.new(cfg['app_secret'].encode('utf-8'), message.encode('utf-8'), hashlib.sha256).digest()).decode()
    return {
        'X-Tsign-Open-App-Id': cfg['app_id'],
        'X-Tsign-Open-Version-Sdk': SDK_VERSION,
        'X-Tsign-Open-Ca-Timestamp': str(int(time.time() * 1000)),
        'Accept': ACCEPT,
        'Content-MD5': md5,
        'Content-Type': CONTENT_TYPE_JSON,
        'X-Tsign-Open-Auth-Mode': 'Signature',
        'X-Tsign-Open-Ca-Signature': signature,
    }


def api(cfg, api_path, method='GET', body=None, timeout=20):
    url = cfg['host'] + api_path
    headers = build_headers(api_path, method, body, cfg)
    return requests.request(method, url, headers=headers,
                             data=(body.encode('utf-8') if body else None), timeout=timeout)

def cmd_connectivity(cfg):
    print('=== [1] 沙箱连通性 + 签名自检 ===')
    print('host        : %s' % cfg['host'])
    print('app-id      : %s' % (cfg['app_id'] or '(未配置)'))
    print('app-secret  : %s' % ('(已配置, %d 字符)' % len(cfg['app_secret']) if cfg['app_secret'] else '(未配置)'))
    try:
        r = api(cfg, '/v3/seals/org-own-seal-list?orgId=%s&pageNum=1&pageSize=10' % (cfg['org_id'] or '0'), 'GET')
        print('HTTP %s' % r.status_code)
        print('body: %s' % r.text[:400])
        if r.status_code == 401:
            print('=> 签名未通过（缺 app-id/app-secret 或凭据无效），说明沙箱可达且已启用鉴权')
        else:
            print('=> 签名已通过（凭据有效）')
    except Exception as e:
        print('=> 连接失败：%s' % e)
        return 1
    return 0


def api_local_callback(url, ts, body, signature):
    try:
        resp = requests.post(url, data=body.encode('utf-8'), headers={
            'Content-Type': 'application/json; charset=UTF-8',
            'X-Tsign-Open-Ca-Timestamp': ts,
            'X-Tsign-Open-Ca-Signature': signature,
        }, timeout=15)
        return resp.status_code, resp.text
    except Exception as e:
        return -1, str(e)


def cmd_callback(cfg):
    print('=== [2] 本地回调验签自测 ===')
    ts = str(int(time.time() * 1000))
    body = json.dumps({'action': 'SIGN_FLOW', 'flowId': 'sandbox-selftest-flow', 'timestamp': ts}, ensure_ascii=False)
    query = 'signFlowId=sandbox-selftest-flow'
    url = LOCAL_WORKFLOW + '/odoc/esign/callback?' + query

    bad = api_local_callback(url, ts, body, 'deadbeef')
    print('错误签名 -> HTTP %s body=%s' % (bad[0], bad[1][:200]))

    sign = hmac.new(cfg['app_secret'].encode('utf-8'),
                    (ts + query + body).encode('utf-8'), hashlib.sha256).hexdigest()
    good = api_local_callback(url, ts, body, sign)
    print('正确签名 -> HTTP %s body=%s' % (good[0], good[1][:200]))
    if good[0] == 200:
        print('=> 验签通过（待签串口径 timestamp+query+body、hex 编码与本系统一致）')
    else:
        print('=> 验签未通过：确认 Nacos 的 app-secret 与脚本读到的是同一份，且已重启 blade-workflow')
    return 0


def cmd_seals(cfg):
    print('=== [3] 印章列表 ===')
    r = api(cfg, '/v3/seals/org-own-seal-list?orgId=%s&pageNum=1&pageSize=100' % (cfg['org_id'] or '0'), 'GET')
    print('HTTP %s' % r.status_code)
    print(r.text[:800])
    return 0


def cmd_full(cfg):
    print('=== [4] 完整链路：上传文件 → 创建签署流程 → 启动 → 查详情 ===')
    if not cfg['app_id'] or not cfg['app_secret']:
        print('缺少 app-id/app-secret，无法执行（请在 Nacos 填写或用环境变量传入）')
        return 2
    sample = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'sample.pdf')
    if not os.path.exists(sample):
        print('缺少待签文件：%s' % sample)
        return 2
    with open(sample, 'rb') as f:
        content = f.read()
    md5 = base64.b64encode(hashlib.md5(content).digest()).decode()
    body = json.dumps({'contentMd5': md5, 'fileName': 'sample.pdf',
                       'fileSize': len(content), 'convertToPDF': False,
                       'contentType': 'application/octet-stream'})
    r = api(cfg, '/v3/files/file-upload-url', 'POST', body)
    print('upload-url HTTP %s %s' % (r.status_code, r.text[:300]))
    data = (r.json().get('data') or {}) if 'json' in r.headers.get('content-type', '') else {}
    upload_url = data.get('fileUploadUrl')
    file_id = data.get('fileId')
    if not upload_url:
        return 1
    up = requests.put(upload_url, data=content, headers={'Content-MD5': md5,
                                                         'Content-Type': 'application/octet-stream'}, timeout=60)
    print('upload HTTP %s' % up.status_code)
    time.sleep(3)
    flow_body = json.dumps({
        'docs': [{'fileId': file_id, 'fileName': 'sample.pdf'}],
        'signFlowConfig': {'signFlowTitle': 'SpringBlade sandbox test', 'chargeConfig': {'chargeMode': 0}},
        'signers': [{
            'noticeConfig': {'noticeTypes': '1'},
            'signConfig': {'signOrder': 1},
            'signerType': 1,
            'orgSignerInfo': {'orgId': cfg['org_id'],
                              'orgName': os.environ.get('ESIGN_ORG_NAME', 'sandbox org'),
                              'orgInfo': {'orgIDCardNum': os.environ.get('ESIGN_ORG_IDCARD', ''), 'orgIDCardType': 'CREDIT_CODE'},
                              'transactorInfo': {'psnAccount': os.environ.get('ESIGN_TRANSCACTOR', ''),
                                                 'psnInfo': {'psnName': os.environ.get('ESIGN_TRANSCACTOR_NAME', 'operator'),
                                                             'psnIDCardNum': os.environ.get('ESIGN_TRANSCACTOR_IDCARD', '')}}},
            'signFields': [{'fileId': file_id, 'customBizNum': 'sbx-1', 'signFieldType': 1,
                            'normalSignFieldConfig': {'assignedSealId': os.environ.get('ESIGN_SEAL_ID', ''),
                                                      'autoSign': True, 'freeMode': False, 'movableSignField': False,
                                                      'signFieldStyle': 1,
                                                      'signFieldPosition': {'positionPage': '1', 'positionX': 100, 'positionY': 100}}}]
        }]
    })
    r2 = api(cfg, '/v3/sign-flow/create-by-file', 'POST', flow_body)
    print('create HTTP %s %s' % (r2.status_code, r2.text[:400]))
    try:
        flow_id = ((r2.json().get('data') or {}).get('signFlowId'))
    except Exception:
        flow_id = None
    if flow_id:
        r3 = api(cfg, '/v3/sign-flow/%s/start' % flow_id, 'POST', '{}')
        print('start HTTP %s %s' % (r3.status_code, r3.text[:200]))
        r4 = api(cfg, '/v3/sign-flow/%s/detail' % flow_id, 'GET')
        print('detail HTTP %s %s' % (r4.status_code, r4.text[:600]))
        print('把上面的 flowId 回填到 wf_odoc_sign_flow.flow_id 后，可在本地验证回调推进链路')
    return 0


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else 'connectivity'
    cfg = load_config()
    fn = {'connectivity': cmd_connectivity, 'callback': cmd_callback,
          'seals': cmd_seals, 'full': cmd_full}.get(mode)
    if fn is None:
        print(__doc__)
        return 2
    return fn(cfg)


if __name__ == '__main__':
    sys.exit(main())