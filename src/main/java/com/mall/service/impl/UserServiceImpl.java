package com.mall.service.impl;

import com.mall.common.Const;
import com.mall.common.ServerResponse;
import com.mall.common.TokenCache;
import com.mall.dao.UserMapper;
import com.mall.pojo.User;
import com.mall.service.IUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("iUserService")
public class UserServiceImpl implements IUserService{

    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String username, String password) {

        int resultCount = userMapper.checkUsername(username);
        if(resultCount == 0)
            return ServerResponse.createByErrorMessage("用户名不存在");

        User user = userMapper.selectLogin(username, password);
        if(user == null)
            return ServerResponse.createByErrorMessage("密码错误");


        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登录成功", user);
    }

    public ServerResponse<String> register(User user) {
        int resultCount = userMapper.checkUsername(user.getUsername());
        if(resultCount > 0)
            return ServerResponse.createByErrorMessage("用户名已存在");

        resultCount = userMapper.checkEmail(user.getEmail());
        if(resultCount > 0)
            return ServerResponse.createByErrorMessage("Email存在");

        user.setRole(Const.Role.ROLE_CUSTOMER);

        user.setPassword(user.getPassword());

        resultCount = userMapper.insert(user);
        if(resultCount == 0)
            return ServerResponse.createByErrorMessage("注册失败");

        return ServerResponse.createBySuccessMessage("注册成功");
    }

    public ServerResponse<String> checkValid(String str, String type) {
        if(org.apache.commons.lang3.StringUtils.isNotBlank(type)) {
            if(Const.USERNAME.equals(type)) {
                int resultCount = userMapper.checkUsername(str);
                if(resultCount > 0)
                    return ServerResponse.createByErrorMessage("用户名已存在");
            }

            if(Const.EMAIL.equals(type)) {
                int resultCount = userMapper.checkEmail(str);
                if(resultCount > 0)
                    return ServerResponse.createByErrorMessage("Email存在");
            }

        } else {
            return ServerResponse.createByErrorMessage("参数错误");
        }
        return ServerResponse.createBySuccess("校验成功");
    }

    public ServerResponse<String> selectQuestion(String username) {
        ServerResponse validResponse = this.checkValid(username, Const.USERNAME);
        if(validResponse.isSuccess())
            return ServerResponse.createByErrorMessage("用户不存在");

        String question = userMapper.selectQuestionByUser(username);

        if(StringUtils.isNotBlank(question))
            return ServerResponse.createBySuccess(question);

        return ServerResponse.createByErrorMessage("找回密码问题为空");

    }

    public ServerResponse<String> checkQuestion(String username, String question, String answer) {
        int resultCount = userMapper.checkAnswer(username, question, answer);
        if(resultCount > 0) {
            String forgetToken = UUID.randomUUID().toString();
            TokenCache.setKey(TokenCache.TOKEN_PREFIX+username, forgetToken);
            return ServerResponse.createBySuccess(forgetToken);
        }
        return ServerResponse.createByErrorMessage("问题的答案错误");
    }

    public ServerResponse<String> forgetResetPassword(String username, String newPassword, String forgetToken) {
        if(StringUtils.isBlank(forgetToken))
            return ServerResponse.createByErrorMessage("需要传递token");

        ServerResponse validResponse = this.checkValid(username, Const.USERNAME);
        if(validResponse.isSuccess())
            return ServerResponse.createByErrorMessage("用户不存在");

        String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX+username);
        if(StringUtils.isBlank(token))
            return ServerResponse.createByErrorMessage("token无效");

        if(StringUtils.equals(forgetToken, token)) {
            int rowCount = userMapper.updatePasswordByUsername(username, newPassword);
            if(rowCount > 0)
                return ServerResponse.createBySuccessMessage("修改密码成功");
        } else {
            return ServerResponse.createByErrorMessage("token错误,请重新获取");
        }

        return ServerResponse.createByErrorMessage("修改密码失败");
    }

    public ServerResponse<String> resetPassword(String oldPassword, String newPassword, User user) {
        int resultCount = userMapper.checkPassword(oldPassword, user.getId());
        if(resultCount == 0)
            return ServerResponse.createByErrorMessage("旧密码错误");

        user.setPassword(newPassword);
        int updateCount = userMapper.updateByPrimaryKeySelective(user);

        if(updateCount > 0)
            return ServerResponse.createBySuccessMessage("密码更新成功");

        return ServerResponse.createByErrorMessage("密码更新失败");
    }

    public ServerResponse<User> updateInformation(User user) {
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(), user.getId());
        if(resultCount > 0)
            return ServerResponse.createByErrorMessage("email已存在，请更换");

        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());

        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);

        if(updateCount > 0)
            return ServerResponse.createBySuccessMessage("更新个人信息成功");

        return ServerResponse.createByErrorMessage("更新个人信息失败");
    }

    public ServerResponse<User> getInformation(Integer userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if(user == null)
            return ServerResponse.createBySuccessMessage("找不到当前用户");
        user.setPassword(StringUtils.EMPTY);

        return ServerResponse.createBySuccess(user);
    }

    public ServerResponse checkAdminRole(User user) {
        if(user != null && user.getRole().intValue() == Const.Role.ROlE_ADMIN )
            return ServerResponse.createBySuccess();
        return ServerResponse.createByError();
    }
}
