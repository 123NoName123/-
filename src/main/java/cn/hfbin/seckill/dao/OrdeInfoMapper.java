package cn.hfbin.seckill.dao;

import cn.hfbin.seckill.entity.OrderInfo;

public interface OrdeInfoMapper {
    int deleteByPrimaryKey(Long id);

    int insert(OrderInfo record);

    int insertSelective(OrderInfo record);

    OrderInfo selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(OrderInfo record);

    int updateByPrimaryKey(OrderInfo record);

    int delete(OrderInfo orderInfo);

    OrderInfo select(OrderInfo orderInfo);

    int update(OrderInfo orderInfo, int status);
}