package com.grameenphone.wipro.fmfs.cbp.service;

import com.grameenphone.wipro.exception.AppRuntimeException;
import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.fmfs.cbp.consts.Actions;
import com.grameenphone.wipro.fmfs.cbp.model.api.comm_module.BalanceResponse;
import com.grameenphone.wipro.fmfs.cbp.model.api.comm_module.BalanceResponse.Balance;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionAttributes;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionObject;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Client;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.ClientConfig;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.ClientDivision;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.BilldataValidatorConfigRequest;
import com.grameenphone.wipro.fmfs.cbp.repository.CrudDao;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.ClientConfigRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.ClientDivisionRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.ClientRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.UserRepository;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

import jakarta.transaction.Transactional;

@Service
public class AccountService {
    @Autowired
    ClientRepository clientRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ClientDivisionRepository clientDivisionRepository;
    
    @Autowired
    ClientConfigRepository clientConfigRepository;

    @Autowired
    BillDataService billDataService;

    @Autowired
    AuthService authService;

    @Value("${flex.mfs.communicator.module.url}")
    String communicatorModuleUrl;

    @Value("${flex.mfs.communicator.module.timeout}")
    int communicatorModuleTimeout;

    public static final String BILLDATA_VALIDATION_CONFIG_KEY = "billdata_validation_enabled";

    protected static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    public boolean isNameExist(String name) {
        SessionObject session = SessionAttributes.current();
        if(session.IS_GP) {
            return CrudDao.get(Client.class).query().eq("name", name).count() > 0;
        } else {
            return CrudDao.get(ClientDivision.class).query().eq("name", name).eq("client", session.getUser().getClient()).count() > 0;
        }
    }

    public boolean isMsisdnExist(long msisdn) {
        int _msisdn = Integer.parseInt(StringUtil.sanitizeMsisdn("" + msisdn));
        return CrudDao.get(Client.class).query().eq("msisdn", _msisdn).count() > 0;
    }

    public void createAccount(String name, Long mobileNo, String address1, String address2, String description) {
        if(isNameExist(name)) {
            throw new AppRuntimeException("Account name must be unique");
        }
        SessionObject session = SessionAttributes.current();
        if(session.IS_GP) {
            if (mobileNo == null || isMsisdnExist(mobileNo)) {
                throw new AppRuntimeException("MSISDN Already Exists For Other Account");
            }
            String mobileAsString = StringUtil.sanitizeMsisdn("" + mobileNo);
            if (mobileAsString == null) {
                throw new AppRuntimeException("MSISDN is Invalid");
            }
            int _mobile = Integer.parseInt(mobileAsString);
            Client client = new Client();
            client.setName(name);
            client.setDescription(description);
            client.setMsisdn(_mobile);
            client.setAddress1(address1);
            client.setAddress2(address2);
            clientRepository.save(client);
        } else {
            ClientDivision division = new ClientDivision();
            division.setName(name);
            division.setDescription(description);
            division.setClient(session.getUser().getClient());
            division = clientDivisionRepository.save(division);
            if(!session.getUser().isAllowAllDivision()) {
                session.getUser().getClientDivisions().add(division);
                userRepository.save(session.getUser());
            }
        }
    }

    public void barUnbarAccount(long accountId, boolean unbar) throws HttpErrorResponseException {
        SessionObject session = SessionAttributes.current();
        if(!session.IS_GP) {
            throw new HttpErrorResponseException(403, null, "You are not authorized for this action");
        }
        Client client = CrudDao.get(Client.class).findOne(accountId);
        client.setActive(unbar);
        clientRepository.save(client);
    }

    public Balance checkBalance(Long accountId) throws IOException {
        SessionObject session = SessionAttributes.current();
        if(accountId == null && session.IS_GP) {
            throw new AppRuntimeException("Please choose an account");
        }
        Client client;
        if(!session.IS_GP) {
            client = session.getUser().getClient();
        } else {
            client = CrudDao.get(Client.class).findOne(accountId);
        }

        HttpClient http = new HttpClient(communicatorModuleTimeout);
        BalanceResponse commResponse = http.getForEntity(communicatorModuleUrl + "wallet/balance/CBP/RET/" + client.getMsisdn(), BalanceResponse.class);
        return commResponse.response;
    }

    public List<Client> getAllActive() {
        return CrudDao.get(Client.class).query().eq("active", true).findAll();
    }
    
    @Transactional
    public void updateAccountValidators(BilldataValidatorConfigRequest request) {
    	User user = SessionAttributes.current().getUser();
    	Client client = user.getClient();
        ClientConfig clientConfig = CrudDao.get(ClientConfig.class).query().eq("clientId", client.getId()).eq("key", BILLDATA_VALIDATION_CONFIG_KEY).findOne();

		if (clientConfig == null) {
        	clientConfig = new ClientConfig();
            clientConfig.setKey(BILLDATA_VALIDATION_CONFIG_KEY);
		    clientConfig.setClientId(client.getId());
        }
        clientConfig.setValue("" + request.isEnabled);
		clientConfigRepository.save(clientConfig);
        authService.allowUserNRolesForAction(request.allowedUsers, request.allowedRoles, Actions.VALIDATE_BILL_DATA);
    }
    
    public boolean isBilldataValidationEnabled() {
    	User user = SessionAttributes.current().getUser();
    	Client client = user.getClient();
        ClientConfig clientConfig = CrudDao.get(ClientConfig.class).query().eq("clientId", client.getId()).eq("key", BILLDATA_VALIDATION_CONFIG_KEY).findOne();
        if(clientConfig != null) {
            return "true".equals(clientConfig.getValue());
        }
		return false;
    }
}