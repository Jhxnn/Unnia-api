package com.Unnia.shared.infrastructure;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Torna cada caso de uso transacional sem anotar a camada de aplicação: o método {@code execute}
 * roda em uma transação, que sofre rollback em qualquer exceção não verificada.
 */
class UseCaseTransactionPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	private static final String USE_CASE_PACKAGE = ".application.usecase.";

	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (!isUseCase(bean)) {
			return bean;
		}
		NameMatchTransactionAttributeSource attributes = new NameMatchTransactionAttributeSource();
		attributes.addTransactionalMethod("execute", new RuleBasedTransactionAttribute());
		TransactionInterceptor interceptor = new TransactionInterceptor();
		interceptor.setTransactionAttributeSource(attributes);
		interceptor.setBeanFactory(beanFactory);

		ProxyFactory proxy = new ProxyFactory(bean);
		proxy.setProxyTargetClass(true);
		proxy.addAdvice(interceptor);
		return proxy.getProxy();
	}

	private boolean isUseCase(Object bean) {
		String name = bean.getClass().getName();
		return name.contains(USE_CASE_PACKAGE) && name.endsWith("UseCase");
	}
}
