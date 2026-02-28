package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
@Slf4j
@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;


    /*
    * 新增菜品和对应的口味
    * */
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        //像菜品表插入一条数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);
        System.out.println("dish的id为" + dish.getId());

        //获取insert语句生成的主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null&& flavors.size()>0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            //像口味表插入n条数据
            try {
                dishFlavorMapper.insertBatch(flavors); // 这里出问题
            } catch (Exception e) {
                log.error("插入口味数据失败: {}", e.getMessage(), e);
                throw e; // 必须抛出，让事务回滚（也方便定位）
            }
        }
    }

}

