package config;

@Configuration
public class ReactiveConfig {

    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager txManager) {
        return TransactionalOperator.create(txManager);
    }

    @Bean
    public ReactiveTransactionManager reactiveTransactionManager(EntityManagerFactory emf) {
        return new ReactiveTransactionManager() {
            @Override
            public Mono<Transaction> getReactiveTransaction(TransactionDefinition definition) {
                return Mono.error(new UnsupportedOperationException("JPA is not reactive, but wrapped here"));
            }
            @Override
            public Mono<Void> commit(Transaction transaction) { return Mono.empty(); }
            @Override
            public Mono<Void> rollback(Transaction transaction) { return Mono.empty(); }
        };
    }
}