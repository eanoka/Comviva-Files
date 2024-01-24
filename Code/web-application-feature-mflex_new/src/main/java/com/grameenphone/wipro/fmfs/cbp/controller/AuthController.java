package com.grameenphone.wipro.fmfs.cbp.controller;

import com.grameenphone.wipro.fmfs.cbp.model.data.session.FrontEndUser;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionAttributes;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionObject;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
	public FrontEndUser userDetails() {
		SessionObject object = SessionAttributes.current();
		FrontEndUser frontEndUser = new FrontEndUser();
		User user = object.getUser();
		frontEndUser.session = object.FRONTEND_UNIQUE;
		frontEndUser.email = user.getEmail();
		frontEndUser.adid = user.getAdid();
		frontEndUser.name = user.getName();
		frontEndUser.id = user.getId();
		frontEndUser.permissions = object.getPermissions();
		frontEndUser.isGP = object.IS_GP;
		frontEndUser.client = user.getClient();
		frontEndUser.role = user.getRole();
		return frontEndUser;
	}
}