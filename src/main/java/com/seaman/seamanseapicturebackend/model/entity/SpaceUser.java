package com.seaman.seamanseapicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 空间用户关联
 *
 * @TableName space_user
 */
@TableName(value = "space_user")
@Data
public class SpaceUser implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 空间 id
     */
    private Long spaceId;
    /**
     * 用户 id
     */
    private Long userId;
    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
}