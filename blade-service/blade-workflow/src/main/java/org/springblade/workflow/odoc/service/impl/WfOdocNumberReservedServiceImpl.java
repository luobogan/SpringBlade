package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.workflow.odoc.entity.WfOdocNumberReserved;
import org.springblade.workflow.odoc.mapper.WfOdocNumberReservedMapper;
import org.springblade.workflow.odoc.service.IWfOdocNumberReservedService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文号保留号服务实现（直接基于 Mapper，未继承 MP IService）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocNumberReservedServiceImpl implements IWfOdocNumberReservedService {

	private final WfOdocNumberReservedMapper reservedMapper;

	@Override
	public Set<Integer> activeSerialSet(Long numberId) {
		List<WfOdocNumberReserved> list = reservedMapper.selectList(Wrappers.<WfOdocNumberReserved>lambdaQuery()
			.eq(WfOdocNumberReserved::getNumberId, numberId)
			.and(w -> w.isNull(WfOdocNumberReserved::getExpireTime)
				.or().gt(WfOdocNumberReserved::getExpireTime, LocalDateTime.now())));
		return list.stream().map(WfOdocNumberReserved::getSerial).collect(Collectors.toSet());
	}

	@Override
	public List<WfOdocNumberReserved> listByNumberId(Long numberId) {
		return reservedMapper.selectList(Wrappers.<WfOdocNumberReserved>lambdaQuery()
			.eq(WfOdocNumberReserved::getNumberId, numberId).orderByAsc(WfOdocNumberReserved::getSerial));
	}

	@Override
	public int reserveRange(Long numberId, int startSerial, int endSerial, LocalDateTime expireTime) {
		if (numberId == null) {
			throw new IllegalArgumentException("numberId 不能为空");
		}
		if (startSerial > endSerial) {
			int t = startSerial;
			startSerial = endSerial;
			endSerial = t;
		}
		Set<Integer> have = reservedMapper.selectList(Wrappers.<WfOdocNumberReserved>lambdaQuery()
				.eq(WfOdocNumberReserved::getNumberId, numberId))
			.stream()
			.map(WfOdocNumberReserved::getSerial)
			.collect(Collectors.toSet());
		Long reservedBy = AuthUtil.getUserId();
		List<WfOdocNumberReserved> toSave = new ArrayList<>();
		for (int s = startSerial; s <= endSerial; s++) {
			if (have.contains(s)) {
				continue;
			}
			WfOdocNumberReserved r = new WfOdocNumberReserved();
			r.setNumberId(numberId);
			r.setSerial(s);
			r.setReservedBy(reservedBy);
			r.setExpireTime(expireTime);
			toSave.add(r);
		}
		int cnt = 0;
		for (WfOdocNumberReserved r : toSave) {
			reservedMapper.insert(r);
			cnt += 1;
		}
		return cnt;
	}

	@Override
	public boolean removeReserved(Long id) {
		return reservedMapper.deleteById(id) > 0;
	}
}
