/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.www.action;

import com.opensymphony.xwork2.ActionContext;
import net.sf.odinms.net.channel.www.WebUser;
import org.apache.struts2.json.annotations.JSON;

/**
 *
 * @author Administrator
 */
public class UserLoginAction {

    private static final String RESULT_COMMAND = "resultCode";
    private WebUser user = new WebUser();
    private int resultCode;
    private String resultStr;
    private String certCode;

    public UserLoginAction() {
    }

    public int getResultCode() {
        return resultCode;
    }

    public String getResultStr() {
        return resultStr;
    }

    @JSON(serialize = false)
    public WebUser getUser() {
        return user;
    }

    public void setCertCode(String certCode) {
        this.certCode = certCode;
    }

    public String Login() throws Exception {
        if (user.checkUserPass()) {
            resultCode = -1;
            return RESULT_COMMAND;
        }
        resultCode = 0;
        String e_certCode = (String) ActionContext.getContext().getSession().get(com.google.code.kaptcha.Constants.KAPTCHA_SESSION_KEY);
        if (certCode == null || !certCode.equalsIgnoreCase(e_certCode)) {
            resultStr = "3";
            return RESULT_COMMAND;
        }
        resultStr = user.Login(ActionContext.getContext().getSession());
        return RESULT_COMMAND;
    }
}
