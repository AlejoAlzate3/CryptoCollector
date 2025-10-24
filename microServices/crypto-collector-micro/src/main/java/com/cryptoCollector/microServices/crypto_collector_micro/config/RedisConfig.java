package com.cryptoCollector.microServices.crypto_collector_micro.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

        private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                logger.info("🔧 Configurando Redis CacheManager con TTLs personalizados...");

                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(5))
                                .serializeKeysWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(
                                                                new GenericJackson2JsonRedisSerializer(objectMapper)))
                                .disableCachingNullValues();

                // Configuraciones específicas por nombre de caché
                Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

                // Lista de criptomonedas - 5 minutos
                cacheConfigurations.put("crypto-list", defaultConfig.entryTtl(Duration.ofMinutes(5)));

                // Detalles de criptomoneda individual - 2 minutos
                cacheConfigurations.put("crypto-details", defaultConfig.entryTtl(Duration.ofMinutes(2)));

                // Estadísticas generales - 1 minuto
                cacheConfigurations.put("crypto-stats", defaultConfig.entryTtl(Duration.ofMinutes(1)));

                // Respuestas de CoinGecko API - 30 segundos (para respetar rate limiting)
                cacheConfigurations.put("coingecko-api", defaultConfig.entryTtl(Duration.ofSeconds(30)));

                // Estado del scheduler - 1 minuto
                cacheConfigurations.put("scheduler-status", defaultConfig.entryTtl(Duration.ofMinutes(1)));

                logger.info("✅ Redis Cache configurado:");
                logger.info("   - crypto-list: 5 minutos TTL");
                logger.info("   - crypto-details: 2 minutos TTL");
                logger.info("   - crypto-stats: 1 minuto TTL");
                logger.info("   - coingecko-api: 30 segundos TTL");
                logger.info("   - scheduler-status: 1 minuto TTL");

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withInitialCacheConfigurations(cacheConfigurations)
                                .transactionAware()
                                .build();
        }

        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
                RedisTemplate<String, Object> template = new RedisTemplate<>();
                template.setConnectionFactory(connectionFactory);
                template.setKeySerializer(new StringRedisSerializer());
                template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
                template.setHashKeySerializer(new StringRedisSerializer());
                template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

                logger.info("✅ RedisTemplate configurado con serializadores JSON");

                return template;
        }
}
