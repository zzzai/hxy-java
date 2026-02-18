package com.zbkj.service.exception;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.exception.ExceptionLog;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.ExceptionLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

/**
 * 全局参数、异常拦截。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private ExceptionLogService exceptionLogService;

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({BindException.class})
    public CommonResult bindException(HttpServletRequest request, BindException e) {
        doLog(request, e);
        BindingResult bindingResult = e.getBindingResult();
        return CommonResult.failed(Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage());
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult bindException(HttpServletRequest request, MethodArgumentNotValidException e) {
        doLog(request, e);
        BindingResult bindingResult = e.getBindingResult();
        return CommonResult.failed(Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage());
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public CommonResult bindException(HttpServletRequest request, HttpMediaTypeNotSupportedException e) {
        doLog(request, e);
        return CommonResult.failed().setMessage(Objects.requireNonNull(e.getMessage()));
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    public CommonResult handleBadSqlGrammarException(HttpServletRequest request, BadSqlGrammarException e) {
        doLog(request, e);
        return CommonResult.failed().setMessage("服务器数据异常，请联系管理员");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public CommonResult handleDataIntegrityViolationException(HttpServletRequest request, DataIntegrityViolationException e) {
        doLog(request, e);
        return CommonResult.failed().setMessage("服务器数据异常，请联系管理员");
    }

    @ExceptionHandler(UncategorizedSQLException.class)
    public CommonResult handleUncategorizedSqlException(HttpServletRequest request, UncategorizedSQLException e) {
        doLog(request, e);
        return CommonResult.failed().setMessage("服务器数据异常，请联系管理员");
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public CommonResult<?> defaultExceptionHandler(HttpServletRequest request, Exception e) {
        doLog(request, e);
        if (e instanceof CrmebException) {
            return CommonResult.failed((CrmebException) e);
        }
        if (e instanceof MissingServletRequestParameterException) {
            return CommonResult.failed().setMessage(Objects.requireNonNull(e.getMessage()));
        }
        return CommonResult.failed().setMessage(e.getMessage());
    }

    private void doLog(HttpServletRequest request, Exception e) {
        log.error("捕获到异常：", e);
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));
        String expDetail = sw.toString();
        try {
            sw.close();
        } catch (IOException ioException) {
            log.error("异常日志：关闭异常详情Writer异常", ioException);
        }

        String expUrl = request.getRequestURI();
        String expType = e.getClass().getName();
        StackTraceElement stackTraceElement = e.getStackTrace()[0];
        String expController = stackTraceElement.getClassName();
        String expMethod = stackTraceElement.getMethodName();

        ExceptionLog exceptionLog = new ExceptionLog();
        exceptionLog.setExpUrl(expUrl);
        exceptionLog.setExpParams("");
        exceptionLog.setExpType(expType);
        exceptionLog.setExpController(expController);
        exceptionLog.setExpMethod(expMethod);
        exceptionLog.setExpDetail(expDetail);
        exceptionLogService.save(exceptionLog);
    }
}

