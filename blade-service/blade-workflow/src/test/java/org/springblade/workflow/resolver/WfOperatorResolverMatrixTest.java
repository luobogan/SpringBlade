package org.springblade.workflow.resolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springblade.core.tool.api.R;
import org.springblade.system.feign.IDeptClient;
import org.springblade.system.user.entity.User;
import org.springblade.system.user.entity.UserInfo;
import org.springblade.system.user.feign.IUserClient;
import org.springblade.workflow.config.WfRetirementProperties;
import org.springblade.workflow.entity.WfNodeOperator;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.mapper.WfFormSnapshotMapper;
import org.springblade.workflow.mapper.WfNodeOperatorMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.matrix.service.IWfMatrixService;
import org.springblade.workflow.matrix.vo.MatrixMatchVO;
import org.springblade.workflow.resolver.WfOperatorResolver;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * WfOperatorResolver 矩阵分支（opType=99）单测，重点覆盖 -13 创建人分部条件解析。
 *
 * <p>纯 Mockito，不依赖 MySQL / Flowable / Nacos。</p>
 */
@ExtendWith(MockitoExtension.class)
class WfOperatorResolverMatrixTest {

	@Mock WfNodeOperatorMapper operatorMapper;
	@Mock WfProcessNodeMapper nodeMapper;
	@Mock WfFormSnapshotMapper snapshotMapper;
	@Mock IUserClient userClient;
	@Mock IWfMatrixService matrixService;
	@Mock WfBpmnExtensionReader bpmnReader;
	@Mock IDeptClient deptClient;
	@Mock WfRetirementProperties retirement;

	@InjectMocks WfOperatorResolver resolver;

	private WfNodeOperator matrixOp99(String matrixId, String matrixJson) {
		WfNodeOperator op = new WfNodeOperator();
		op.setOpType(99);
		op.setObjId(matrixId);
		op.setMatrixJson(matrixJson);
		return op;
	}

	private void stubFallbackTo(List<WfNodeOperator> ops) {
		// 走 wf_node_operator 回退读（operatorFromBpmn=false 且 retirement 未启用）
		lenient().when(retirement.isEnabled()).thenReturn(false);
		WfProcessNode node = new WfProcessNode();
		node.setId(5L);
		lenient().when(nodeMapper.selectOne(any())).thenReturn(node);
		lenient().when(operatorMapper.selectList(any())).thenReturn(ops);
	}

	private void stubStarter(Long starter, String deptId) {
		UserInfo ui = new UserInfo();
		User u = new User();
		u.setDeptId(deptId);
		ui.setUser(u);
		lenient().when(userClient.userInfo(starter)).thenReturn(R.data(ui));
	}

	@Test
	@DisplayName("-13 创建人分部：部门 → 所属分部后参与矩阵匹配")
	void matrixByCreatorDivision() {
		Long starter = 3L;
		stubStarter(starter, "20000000003");
		lenient().when(deptClient.divisionIdByDept(20000000003L)).thenReturn(R.data(50000000001L));

		Long matrixId = 99L;
		String matrixJson = "{\"matrixId\":\"99\",\"valueField\":\"owner\","
			+ "\"conditions\":[{\"condField\":\"div\",\"wfField\":\"-13\"}]}";
		MatrixMatchVO vo = new MatrixMatchVO();
		vo.setUserIds(List.of(7L));
		lenient().when(matrixService.matchUsers(eq(matrixId), eq("owner"), eq(Map.of("div", "50000000001"))))
			.thenReturn(vo);
		lenient().when(userClient.activeStatusMap(any())).thenReturn(R.data(Map.of(7L, true)));

		stubFallbackTo(List.of(matrixOp99(matrixId.toString(), matrixJson)));

		List<Long> ids = resolver.resolve(1L, "node1", null, starter, null);
		assertEquals(List.of(7L), ids, "-13 应解析出创建人分部并命中矩阵");
	}

	@Test
	@DisplayName("-13 分部解析失败（组织服务返回 null）→ 条件不参与匹配 → 矩阵未命中")
	void matrixByCreatorDivisionMissing() {
		Long starter = 3L;
		stubStarter(starter, "20000000003");
		lenient().when(deptClient.divisionIdByDept(20000000003L)).thenReturn(R.data(null));

		Long matrixId = 99L;
		String matrixJson = "{\"matrixId\":\"99\",\"valueField\":\"owner\","
			+ "\"conditions\":[{\"condField\":\"div\",\"wfField\":\"-13\"}]}";
		MatrixMatchVO vo = new MatrixMatchVO(); // 空（未命中）
		lenient().when(matrixService.matchUsers(eq(matrixId), eq("owner"), eq(Map.of())))
			.thenReturn(vo);

		stubFallbackTo(List.of(matrixOp99(matrixId.toString(), matrixJson)));

		List<Long> ids = resolver.resolve(1L, "node1", null, starter, null);
		assertTrue(ids.isEmpty(), "-13 取值失败时按无值处理，矩阵不命中");
	}
}
