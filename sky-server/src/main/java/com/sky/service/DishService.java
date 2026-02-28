package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import org.springframework.stereotype.Service;

import java.util.List;


public interface DishService {
    public void saveWithFlavor(DishDTO dishDTO);
}
