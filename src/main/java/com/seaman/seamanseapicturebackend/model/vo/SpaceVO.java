package com.seaman.seamanseapicturebackend.model.vo;

import com.seaman.seamanseapicturebackend.model.entity.Space;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 空间视图类
 */
@Data
public class SpaceVO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
    /**
     * 空间名称
     */
    private String spaceName;
    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;
    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;
    /**
     * 空间图片的最大数量
     */
    private Long maxCount;
    /**
     * 当前空间下图片的总大小
     */
    private Long totalSize;
    /**
     * 当前空间下的图片数量
     */
    private Long totalCount;
    /**
     * 创建用户 id
     */
    private Long userId;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 编辑时间
     */
    private Date editTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 创建用户信息
     */
    private UserVO user;
    /**
     * 空间类型：0-私有 1-团队
     */
    private Integer spaceType;

    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();



    /**
     * 封装类转对象
     *
     * @param spaceVO
     * @return
     */
    public static Space voToObj(SpaceVO spaceVO) {
        if (spaceVO == null) {
            return null;
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceVO, space);
        return space;
    }

    /**
     * 对象转封装类
     *
     * @param space
     * @return
     */
    public static SpaceVO objToVo(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space, spaceVO);
        return spaceVO;
    }
}
