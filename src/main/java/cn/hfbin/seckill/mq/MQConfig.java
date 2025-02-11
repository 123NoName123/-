package cn.hfbin.seckill.mq;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MQConfig {
	
	public static final String MIAOSHA_QUEUE = "seckill.queue";
	public static final String QUEUE = "queue";
	public static final String DEAD_LETTER_EXCHANGE = "seckill.dead.letter.exchange";
	public static final String MIAOSHA_DEAD_LETTER_QUEUE = "seckill.dead.letter.queue";
	private static final String MIAOSHA_DEAD_LETTER_ROUNTING_KEY = "seckill.dead.letter.key";
	private static final String CANCLE_ORDER_ROUNTING_KEY = "cancle.order.key";
	public static final String SECKILL_CANCLE_ORDER_DEALY_QUEUE = "seckill.cancle.orde.delay.queue";
	public static final String SECKILL_CANCLE_ORDER_QUEUE = "seckill.cancle.orde.queue";
	/*public static final String TOPIC_QUEUE1 = "topic.queue1";
	public static final String TOPIC_QUEUE2 = "topic.queue2";
	public static final String HEADER_QUEUE = "header.queue";
	public static final String TOPIC_EXCHANGE = "topicExchage";
	public static final String FANOUT_EXCHANGE = "fanoutxchage";
	public static final String HEADERS_EXCHANGE = "headersExchage";*/

	@Bean
	public MessageConverter getMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}
	@Bean
	public Queue queue() {
		// 创建秒杀队列，并设置死信队列相关属性
		Map<String, Object> args = new HashMap<>();
		// 配置死信交换机
		args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
		// 配置死信路由键
		args.put("x-dead-letter-routing-key", MIAOSHA_DEAD_LETTER_ROUNTING_KEY );
		return new Queue(MIAOSHA_QUEUE, true, false, false, args);
	}


	/**
	 * Direct模式 交换机Exchange
	 * */
	/*@Bean
	public Queue queue() {
		return new Queue(QUEUE, true);
	}
	@Bean
	public DirectExchange topicDirect(){
		return new DirectExchange(TOPIC_EXCHANGE);
	}
	
	*//**
	 * Topic模式 交换机Exchange
	 * *//*
	@Bean
	public Queue topicQueue1() {
		return new Queue(TOPIC_QUEUE1, true);
	}
	@Bean
	public Queue topicQueue2() {
		return new Queue(TOPIC_QUEUE2, true);
	}
	@Bean
	public TopicExchange topicExchage(){
		return new TopicExchange(TOPIC_EXCHANGE);
	}
	@Bean
	public Binding topicBinding1() {
		return BindingBuilder.bind(topicQueue1()).to(topicExchage()).with("topic.key1");
	}
	@Bean
	public Binding topicBinding2() {
		return BindingBuilder.bind(topicQueue2()).to(topicExchage()).with("topic.#");
	}
	*//**
	 * Fanout模式 交换机Exchange
	 * *//*
	@Bean
	public FanoutExchange fanoutExchage(){
		return new FanoutExchange(FANOUT_EXCHANGE);
	}
	@Bean
	public Binding FanoutBinding1() {
		return BindingBuilder.bind(topicQueue1()).to(fanoutExchage());
	}
	@Bean
	public Binding FanoutBinding2() {
		return BindingBuilder.bind(topicQueue2()).to(fanoutExchage());
	}
	*//**
	 * Header模式 交换机Exchange
	 * *//*
	@Bean
	public HeadersExchange headersExchage(){
		return new HeadersExchange(HEADERS_EXCHANGE);
	}
	@Bean
	public Queue headerQueue1() {
		return new Queue(HEADER_QUEUE, true);
	}
	@Bean
	public Binding headerBinding() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("header1", "value1");
		map.put("header2", "value2");
		return BindingBuilder.bind(headerQueue1()).to(headersExchage()).whereAll(map).match();
	}
	*/
	@Bean
	public DirectExchange deadLetterExchange() {
		return new DirectExchange(DEAD_LETTER_EXCHANGE);
	}

	@Bean
	public Queue miaoshaDeadLetterQueue() {
		return new Queue(MIAOSHA_DEAD_LETTER_QUEUE, true);
	}
	@Bean
	public Binding bindingDeadLetterQueue() {
        return BindingBuilder.bind(miaoshaDeadLetterQueue()).to(deadLetterExchange()).with(MIAOSHA_DEAD_LETTER_ROUNTING_KEY);
	}
	@Bean
	public Binding bindingCancleOrderQueue() {
        return BindingBuilder.bind(cancleOrderQueue()).to(deadLetterExchange()).with(CANCLE_ORDER_ROUNTING_KEY);
	}

	@Bean
	public Queue miaoshaCancleOrderDelayQueue() {
		// 配置延时队列参数
		Map<String, Object> argsMap = new HashMap<>();
		argsMap.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE); // 绑定死信交换机
		argsMap.put("x-dead-letter-routing-key", CANCLE_ORDER_ROUNTING_KEY); // 绑定死信 RoutingKey
		argsMap.put("x-message-ttl", 30 * 60 * 1000); // 消息 TTL 10 秒
		Queue queue = new Queue(SECKILL_CANCLE_ORDER_DEALY_QUEUE, true, false, false, argsMap);
		return queue;
	}
	@Bean
	public Queue cancleOrderQueue() {
		return new Queue(SECKILL_CANCLE_ORDER_QUEUE);
	}
}
