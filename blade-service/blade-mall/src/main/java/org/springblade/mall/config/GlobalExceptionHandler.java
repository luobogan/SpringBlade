package org.springblade.mall.config;

import org.springblade.mall.exception.CouponNotFoundException;
import org.springblade.core.tool.api.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理优惠券不存在异常
     * @param ex CouponNotFoundException
     * @return 错误响应
     */
    @ExceptionHandler(CouponNotFoundException.class)
    public ResponseEntity<R<?>> handleCouponNotFoundException(CouponNotFoundException ex) {
        logger.error("优惠券不存在异常: {}", ex.getMessage(), ex);
        R<?> responseVO = R.fail(ex.getMessage());
        return new ResponseEntity<>(responseVO, HttpStatus.NOT_FOUND);
    }

    /**
     * 处理运行时异常
     * @param ex RuntimeException
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<R<?>> handleRuntimeException(RuntimeException ex) {
        logger.error("运行时异常: {}", ex.getMessage(), ex);
        R<?> responseVO = R.fail(ex.getMessage());
        return new ResponseEntity<>(responseVO, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理所有异常
     * @param ex Exception
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<?>> handleException(Exception ex) {
        logger.error("服务器内部错误: {}", ex.getMessage(), ex);
        R<?> responseVO = R.fail("服务器内部错误");
        return new ResponseEntity<>(responseVO, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}



