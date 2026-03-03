package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import com.sky.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Slf4j
@Api("C端购物车相关接口")
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    @PostMapping("/add")
    @ApiOperation("添加购物车")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("添加购物车相关信息:{}",shoppingCartDTO);
        shoppingCartService.add(shoppingCartDTO);
        return Result.success(shoppingCartDTO);
    }


    @GetMapping("/list")
    @ApiOperation("查看购物车")
    public Result<List<ShoppingCart>> list(){
        List<ShoppingCart> shoppingCarts = shoppingCartService.ShowShoppingCart();
        return Result.success(shoppingCarts);
    }

    @DeleteMapping("/clean")
    @ApiOperation("清空购物车")
    public Result delete(){
        log.info("清空购物车");
        shoppingCartService.delete();
        return Result.success();
    }


    @PostMapping("/sub")
    @ApiOperation("单独删除购物车数据")
    public Result deleteById(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("<UNK>:单独删除购物车数据{}",shoppingCartDTO);
        shoppingCartService.deleteById(shoppingCartDTO);
        return Result.success();
    }
}
