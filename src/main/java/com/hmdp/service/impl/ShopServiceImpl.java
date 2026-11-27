package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopById(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 1.从Redis中查询店铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.如果存在，返回店铺信息
        if (StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shopJson);
        }
        // 3.如果不存在，从数据库中查询
        Shop shop = getById(id);
        // 4.数据库不存在，返回错误信息
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        // 5.数据库存在，返回店铺信息,并写入Redis,设置缓存时间为 30分钟
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 6.返回结果
        return Result.ok(shop);
    }

}
