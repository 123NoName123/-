package cn.hfbin.seckill;

import cn.hfbin.seckill.bo.GoodsBo;
import cn.hfbin.seckill.dao.GoodsMapper;
import cn.hfbin.seckill.dao.OrdeInfoMapper;
import cn.hfbin.seckill.dao.SeckillOrderMapper;
import cn.hfbin.seckill.entity.Goods;
import cn.hfbin.seckill.entity.OrderInfo;
import cn.hfbin.seckill.entity.SeckillOrder;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class SeckillApplicationTests {

	@Autowired
	DataSource dataSource;

	@Autowired
	GoodsMapper goodsMapper;

	@Autowired
	OrdeInfoMapper ordeInfoMapper;

	@Autowired
	SeckillOrderMapper seckillOrderMapper;

	@Test
	public void contextLoads() throws SQLException {
		//org.apache.tomcat.jdbc.pool.DataSource
		System.out.println(dataSource.getClass());
		Connection connection = dataSource.getConnection();
		System.out.println(connection);
		connection.close();

	}

	@Test
	public void test01(){
		List<GoodsBo> goodsBos = goodsMapper.selectAllGoodes();

		for (GoodsBo goodsBo : goodsBos){
			log.info(goodsBo+"");
		}
	}
	@Test
	public void test02(){
		OrderInfo orderInfo = new OrderInfo();
		orderInfo.setId(1L);
		ordeInfoMapper.delete(orderInfo);
	}
	@Test
	public void test03(){
		SeckillOrder seckillOrder = new SeckillOrder();
		seckillOrder.setUserId(1410080408L);
		seckillOrderMapper.delete(seckillOrder);
	}

	@Test
	public void test04(){

	}
}
