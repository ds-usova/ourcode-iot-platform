package org.ourcode.deviceservice.persistence.configuration;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ourcode.deviceservice.api.exception.PersistenceException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionSystemException;

@Aspect
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PersistenceExceptionTranslationAspect {

    @Around("@within(TranslatePersistenceExceptions) || @annotation(TranslatePersistenceExceptions)")
    public Object translateException(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (DataAccessException | TransactionSystemException e) {
            log.error("An error occurred in {} method", joinPoint.getSignature(), e);
            throw new PersistenceException("Database operation failed", e);
        }
    }

}
