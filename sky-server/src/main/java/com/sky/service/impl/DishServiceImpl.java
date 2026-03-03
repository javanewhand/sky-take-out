package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class DishServiceImpl implements DishService {


    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setMealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;


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
                dishFlavorMapper.insertBatch(flavors);// 这里出问题

                //清理缓存数据
                String key="dish:"+dishDTO.getId();
                clearCache(key);
            } catch (Exception e) {
                log.error("插入口味数据失败: {}", e.getMessage(), e);
                throw e; // 必须抛出，让事务回滚（也方便定位）
            }
        }
    }

    /*分页查询*/
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page=dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }


    /*批量删除菜品*/
    @Transactional
    public void delete(List<Long> ids) {
        //判断当前菜品是否在起售中
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus() == StatusConstant.ENABLE){
                log.error("触发状态异常");
                //当前菜品不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }

        }

        //判断当前菜品是否被其它套餐关联
        List<Long> setMealIds = setMealDishMapper.getSetMealDishIds(ids);
        log.info("查询到的关联套餐为：{}", setMealIds);
        if(setMealIds.size()>0&&setMealIds != null){
            log.error("出发关联异常");
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品数据
        for (Long id : ids) {
            dishMapper.delete(id);
            //删除口味数据
            dishFlavorMapper.deleteByDishId(id);
        }
        //清理缓存数据
        clearCache("dish:*");

    }

    @Override
    public DishVO selectByIdWithFlavor(Long id) {
        //根据ID查询dish的数据
         Dish dish = dishMapper.selectById(id);
        //根据id查询与dish相关的口味的数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.selectById(id);
        //将查询的数据封装到VO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }



    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //修改菜品表的信息
        dishMapper.update(dish);
        //清理缓存数据
        clearCache("dish:*");

        //删除原有的口味
        dishFlavorMapper.deleteByDishId(dish.getId());

        //重新插入新的口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null&& flavors.size()>0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            //像口味表插入n条数据
            try {
                dishFlavorMapper.insertBatch(flavors);// 这里出问题
            } catch (Exception e) {
                log.error("插入口味数据失败: {}", e.getMessage(), e);
                throw e; // 必须抛出，让事务回滚（也方便定位）
            }
        }

    }


    //更改菜品状态
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = new Dish();
        dish.setStatus(status);
        dish.setId(id);
        dishMapper.update(dish);
        //清理缓存数据
        clearCache("dish:*");
    }


    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }


    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {

        //构造redis的key，规则：dish_id
        String key="dish:"+ dish.getCategoryId();

        //查询redis中是否存在菜品数据
        List<DishVO> list = (List<DishVO>)redisTemplate.opsForValue().get(key);
        if(list!=null&&list.size()>0){
            //如果存在，直接返回，无需查询数据库
            return list;
        }

        //如果不存在，查询mysql数据库
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.selectById(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }
        //将数据放入redis中
        redisTemplate.opsForValue().set(key,dishVOList);
        return dishVOList;
    }


    private void clearCache(String pattern) {
        Set keys=redisTemplate.keys("dish:*");
        redisTemplate.delete(keys);
        log.info("清理缓存");
    }

}

