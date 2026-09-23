package com.hjq.CodeLess.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.hjq.CodeLess.exception.BusinessException;
import com.hjq.CodeLess.exception.ErrorCode;
import com.hjq.CodeLess.mapper.AppMapper;
import com.hjq.CodeLess.model.dto.user.UserQueryRequest;
import com.hjq.CodeLess.model.dto.user.UserUpdateMyRequest;
import com.hjq.CodeLess.model.entity.App;
import com.hjq.CodeLess.model.enums.UserRoleEnum;
import com.hjq.CodeLess.model.vo.AppVO;
import com.hjq.CodeLess.model.vo.LoginUserVO;
import com.hjq.CodeLess.model.vo.UserVO;
import com.hjq.CodeLess.service.AppService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.hjq.CodeLess.model.entity.User;
import com.hjq.CodeLess.mapper.UserMapper;
import com.hjq.CodeLess.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户 服务层实现。
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService{

    private static final String USER_LOGIN_STATE = "userLoginState";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 检查是否重复
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 根据账号查询用户，再校验带随机盐的密码哈希
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        // 用户不存在
        if (user == null || !matchesPassword(userPassword, user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 兼容历史 MD5 密码：用户成功登录时自动升级为 BCrypt
        if (!isBcryptHash(user.getUserPassword())) {
            User passwordUpgrade = new User();
            passwordUpgrade.setId(user.getId());
            passwordUpgrade.setUserPassword(getEncryptPassword(userPassword));
            this.updateById(passwordUpgrade);
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 4. 获得脱敏后的用户信息
        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }


    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }


    @Override
    public String getEncryptPassword(String userPassword) {
        return BCrypt.hashpw(userPassword, BCrypt.gensalt());
    }

    @Override
    public boolean updateMyProfile(UserUpdateMyRequest userUpdateMyRequest, HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String userName = StrUtil.trim(userUpdateMyRequest.getUserName());
        String userAvatar = StrUtil.trim(userUpdateMyRequest.getUserAvatar());
        String userProfile = StrUtil.trim(userUpdateMyRequest.getUserProfile());
        if (StrUtil.isBlank(userName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称不能为空");
        }
        if (userName.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称最多 50 个字符");
        }
        if (userAvatar != null && userAvatar.length() > 1024) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像地址过长");
        }
        if (StrUtil.isNotBlank(userAvatar) && !isHttpUrl(userAvatar)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像地址必须使用 HTTP 或 HTTPS 协议");
        }
        if (userProfile != null && userProfile.length() > 500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "个人简介最多 500 个字符");
        }

        User loginUser = getLoginUser(request);
        User user = new User();
        user.setId(loginUser.getId());
        user.setUserName(userName);
        user.setUserAvatar(userAvatar);
        user.setUserProfile(userProfile);
        boolean result = this.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "个人资料保存失败");
        }
        return true;
    }

    @Override
    public boolean changePassword(String currentPassword, String newPassword, HttpServletRequest request) {
        if (StrUtil.hasBlank(currentPassword, newPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        if (currentPassword.length() < 8 || currentPassword.length() > 64
                || newPassword.length() < 8 || newPassword.length() > 64) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度应为 8～64 位");
        }
        if (currentPassword.equals(newPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能与当前密码相同");
        }

        User loginUser = getLoginUser(request);
        if (!matchesPassword(currentPassword, loginUser.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前密码错误");
        }

        User user = new User();
        user.setId(loginUser.getId());
        user.setUserPassword(getEncryptPassword(newPassword));
        boolean result = this.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "密码修改失败");
        }
        request.getSession().invalidate();
        return true;
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (StrUtil.isBlank(storedPassword)) {
            return false;
        }
        if (isBcryptHash(storedPassword)) {
            return BCrypt.checkpw(rawPassword, storedPassword);
        }
        final String legacySalt = "codeless";
        String legacyHash = DigestUtils.md5DigestAsHex((legacySalt + rawPassword).getBytes());
        return legacyHash.equals(storedPassword);
    }

    private boolean isBcryptHash(String passwordHash) {
        return passwordHash != null
                && (passwordHash.startsWith("$2a$")
                || passwordHash.startsWith("$2b$")
                || passwordHash.startsWith("$2y$"));
    }

    private boolean isHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

}
