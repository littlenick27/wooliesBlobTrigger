package com.function.blogtriggertest;

public class userFileConfig {

    private String userID;
    private String roleID;	
    private String role_name;	
    private String menu_name;	
    private String screenID;
    private String screen_name;	
    private String access_level;
	private String response;
    
    public userFileConfig(String userID, String roleID, String role_name, String menu_name, String screenID, String screen_name, String access_level, String response) {
    	this.userID = userID;
    	this.roleID = roleID;
    	this.role_name = role_name;
    	this.menu_name = menu_name;
    	this.screenID = screenID;
    	this.screen_name = screen_name;
    	this.access_level = access_level;
        this.response = response;
    }
	public String getUserID() {
		return userID;
	}
	public void setUserID(String userID) {
		this.userID = userID;
	}
	public String getRoleID() {
		return roleID;
	}
	public void setRoleID(String roleID) {
		this.roleID = roleID;
	}
	public String getRole_name() {
		return role_name;
	}
	public void setRole_name(String role_name) {
		this.role_name = role_name;
	}
	public String getMenu_name() {
		return menu_name;
	}
	public void setMenu_name(String menu_name) {
		this.menu_name = menu_name;
	}
	public String getScreenID() {
		return screenID;
	}
	public void setScreenID(String screenID) {
		this.screenID = screenID;
	}
	public String getScreen_name() {
		return screen_name;
	}
	public void setScreen_name(String screen_name) {
		this.screen_name = screen_name;
	}
	public String getAccess_level() {
		return access_level;
	}
	public void setAccess_level(String access_level) {
		this.access_level = access_level;
	}

	public String getResponse() {
		return response;
	}
	public void setResponse(String response) {
		this.response = response;
	}
    
}
