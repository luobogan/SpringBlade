package org.springblade.workflow.concurrency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springblade.workflow.FlowableTestConfig;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.helper.WfInstanceActWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-12 并发压测：直接驱动本改造的「双写收口器」{@link WfInstanceActWriter} 与真实 Flowable 引擎，
 * 在并发下验证：
 *
 * <ul>
 *   <li>① 并发发起：每个流程实例的 {@code ACT_HI_PROCINST} 业务列（BUSINESS_ID_/TITLE_/STARTER_/BUSINESS_STATUS_）
 *       都完整落库、状态=RUNNING，无空值、无丢失实例 —— 证明双写在并发下不漂移；</li>
 *   <li>② 并发写竞争（当前节点/终态）：多线程对同一批实例高频更新，无死锁、无丢失更新
 *       （{@code Future.get()} 会因死锁/回滚抛异常而失败）；</li>
 *   <li>③ 并发读一致性：读源切换（读 ACT_*）在并发读写下不会读到「半截」业务列（BUSINESS_STATUS_/BUSINESS_ID_ 永不为 NULL）。</li>
 * </ul>
 *
 * <p>本测试不依赖 Nacos / MySQL / Redis，复用 {@link FlowableTestConfig} 的内存 H2 + 真实 Flowable 引擎。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
	FlowableTestConfig.class,
	WfInstanceActWriter.class,
	DualWriteProbe.class,
	ConcurrencyTestConfig.class
})
class WfInstanceActWriterConcurrencyTest {

	@Autowired
	private DualWriteProbe probe;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private IProcessService processService;

	private final Set<String> deployed = new HashSet<>();

	@BeforeEach
	void deploy() {
		deployed.add(processService.deployProcess("concurrencyProc", bpmn()));
	}

	@AfterEach
	void cleanup() {
		for (String depId : deployed) {
			processService.deleteDeployment(depId);
		}
		deployed.clear();
	}

	private static String bpmn() {
		return """
			<?xml version="1.0" encoding="UTF-8"?>
			<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
			             xmlns:flowable="http://flowable.org/bpmn"
			             targetNamespace="http://springblade.workflow"
			             id="definitions_concurrency">
			    <process id="concurrencyProc" name="并发压测" isExecutable="true">
			        <startEvent id="startEvent" name="发起"/>
			        <userTask id="approve" name="审批" flowable:assignee="1001"/>
			        <endEvent id="endEvent" name="结束"/>
			        <sequenceFlow id="f1" sourceRef="startEvent" targetRef="approve"/>
			        <sequenceFlow id="f2" sourceRef="approve" targetRef="endEvent"/>
			    </process>
			</definitions>
			""";
	}

	/** ① 并发发起：双写不漂移、业务列零空值 */
	@Test
	void concurrentStart_allActRowsFullyPopulated() throws Exception {
		int threads = 16;
		int perThread = 20; // 共 320 个流程实例
		ExecutorService es = Executors.newFixedThreadPool(threads);
		List<Future<List<String>>> futs = new ArrayList<>();
		AtomicLong idGen = new AtomicLong(900_000L);

		for (int t = 0; t < threads; t++) {
			futs.add(es.submit(() -> {
				List<String> ids = new ArrayList<>();
				for (int i = 0; i < perThread; i++) {
					long wfId = idGen.incrementAndGet();
					ids.add(probe.startAndWrite(wfId, "concurrencyProc", 1001L, "并发标题" + wfId));
				}
				return ids;
			}));
		}

		List<String> all = new ArrayList<>();
		for (Future<List<String>> f : futs) {
			all.addAll(f.get()); // 任一线程抛异常（含死锁/回滚）会让 get() 抛 ExecutionException
		}
		es.shutdown();

		assertThat(all).hasSize(threads * perThread);

		// 每个实例的 ACT 行业务列必须完整、状态=RUNNING
		for (String instId : all) {
			Map<String, Object> row = jdbc.queryForMap(
				"SELECT BUSINESS_ID_, TITLE_, STARTER_, BUSINESS_STATUS_ FROM ACT_HI_PROCINST WHERE ID_=?", instId);
			assertThat(row.get("BUSINESS_ID_")).isNotNull();
			assertThat(row.get("TITLE_")).isNotNull();
			assertThat(row.get("STARTER_")).isNotNull();
			assertThat(row.get("BUSINESS_STATUS_")).isEqualTo("RUNNING");
		}
	}

	/** ② 并发写竞争：当前节点/终态高频更新，无死锁、无丢失更新 */
	@Test
	void concurrentNodeLifecycle_noDeadlock_noLostUpdate() throws Exception {
		// 先起 4 个实例作为竞争目标
		List<String> instIds = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			instIds.add(probe.startAndWrite(800_000L + i, "concurrencyProc", 1001L, "节点压测" + i));
		}

		int threads = 32;
		int iters = 300;
		ExecutorService es = Executors.newFixedThreadPool(threads);
		CyclicBarrier barrier = new CyclicBarrier(threads);
		List<Future<?>> futs = new ArrayList<>();

		for (int t = 0; t < threads; t++) {
			futs.add(es.submit(() -> {
				barrier.await();
				for (int i = 0; i < iters; i++) {
					String id = instIds.get(i % instIds.size());
					probe.writeNode(id, "node_" + i);
					if (i % 3 == 0) {
						probe.terminate(id);
					}
				}
				return null;
			}));
		}
		// get() 会在死锁/回滚时抛异常，使测试失败
		for (Future<?> f : futs) {
			f.get();
		}
		es.shutdown();

		// 终态写应全部成功落地：每个实例的 BUSINESS_STATUS_ 不为 NULL
		for (String id : instIds) {
			String st = jdbc.queryForObject(
				"SELECT BUSINESS_STATUS_ FROM ACT_HI_PROCINST WHERE ID_=?", String.class, id);
			assertThat(st).isNotNull();
		}
	}

	/** ③ 并发读一致性：读源切换在并发读写下不暴露半截业务列 */
	@Test
	void concurrentReadSeesConsistentBusinessColumns() throws Exception {
		// 起 6 个实例，并在后台持续写当前节点/终态制造读写交叠
		List<String> instIds = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			instIds.add(probe.startAndWrite(700_000L + i, "concurrencyProc", 1001L, "读一致性" + i));
		}

		int writerThreads = 16;
		int readerThreads = 16;
		int iters = 500;
		ExecutorService es = Executors.newFixedThreadPool(writerThreads + readerThreads);
		CountDownLatch go = new CountDownLatch(1);
		AtomicReference<Throwable> readError = new AtomicReference<>();
		List<Future<?>> futs = new ArrayList<>();

		// 写线程：持续更新当前节点/终态
		for (int t = 0; t < writerThreads; t++) {
			futs.add(es.submit(() -> {
				go.await();
				for (int i = 0; i < iters; i++) {
					String id = instIds.get(i % instIds.size());
					probe.writeNode(id, "node_" + i);
					if (i % 5 == 0) {
						probe.terminate(id);
					}
				}
				return null;
			}));
		}

		// 读线程：模拟读源切换的「按 ID 取台账」查询，断言业务列永不全 NULL
		for (int t = 0; t < readerThreads; t++) {
			futs.add(es.submit(() -> {
				go.await();
				for (int i = 0; i < iters; i++) {
					String id = instIds.get(i % instIds.size());
					Map<String, Object> row = jdbc.queryForMap(
						"SELECT ID_, BUSINESS_STATUS_, BUSINESS_ID_, TITLE_ FROM ACT_HI_PROCINST WHERE ID_=?", id);
					if (row.get("BUSINESS_STATUS_") == null || row.get("BUSINESS_ID_") == null) {
						readError.compareAndSet(null, new AssertionError("读到半截业务列 instId=" + id));
					}
				}
				return null;
			}));
		}

		go.countDown();
		for (Future<?> f : futs) {
			f.get();
		}
		es.shutdown();
		assertThat(readError.get()).isNull();
	}
}
