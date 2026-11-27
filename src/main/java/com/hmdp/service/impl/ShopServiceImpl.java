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
        // 采用缓存空值的方法来防止缓存击穿
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 1.从Redis中查询店铺缓存 -- 可能获取到的是店铺信息，也可能是空值
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.如果返回的是店铺，返回店铺信息
        if (StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shopJson);
        }
        // 如果返回的是空值，返回错误信息，这就是从数据库保存在Redis里的值：""  表示空字符串
        if (shopJson != null) {
            return Result.fail("店铺不存在");
        }
        // 3.现在可判断店铺在数据库中是否存在
        Shop shop = getById(id);
        // 4.数据库不存在，返回错误信息
        if (shop == null) {
            // 缓存中不存在，数据库中也不存在，向缓存里放空值
            stringRedisTemplate.opsForValue().set(key, "",  RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("店铺不存在");
        }
        // 5.数据库存在，返回店铺信息,并写入Redis,设置缓存时间为 30分钟
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 6.返回结果
        return Result.ok(shop);
    }

    @Override
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 创建redis的键
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 1.更新数据库
        updateById(shop);
        // 2.删除Redis缓存
        stringRedisTemplate.delete(key);
        // 3.返回结果
        return Result.ok();
    }
}
