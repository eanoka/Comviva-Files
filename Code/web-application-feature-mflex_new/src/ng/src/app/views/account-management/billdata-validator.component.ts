import {AfterViewInit, Component} from '@angular/core';
import {BilldataValidatorConfig, BilldataValidatorConfigResponse} from "./billdata-validator-config.model";
import { User } from '../user-management/user.model';
import { Role } from '../role-management/role.model';
import { App } from '../../app.model';
import { ApiPostResponse } from '../../generic/api-post-response.model';
import { Snackbar } from '../blocks/snackbar.component';
import { HttpClient } from '@angular/common/http';
import * as $ from 'jquery';

@Component({
  templateUrl: 'billdata-validator.component.html',
  styleUrls: ['billdata-validator.component.scss'],
  selector: 'bordered-content-wrapper'
})

export class BillDataValidatorComponent implements AfterViewInit  {
    public dataLoaded: boolean
    public dataChanged: boolean = false

    public data: BilldataValidatorConfig = new BilldataValidatorConfig()
    public allUsers: any[] = []
    public allRoles: any[] = []
    
    constructor(private http: HttpClient) {
    }

	ngAfterViewInit(): void {
		this.http.get<BilldataValidatorConfigResponse>(App.basePath + "/account/getBilldataValidatorConfig").subscribe(x => {
			this.allUsers = x.allUsers
			this.allRoles = x.allRoles
			this.data.isEnabled = x.isEnabled
			this.data.allowedUsers = x.allowedUsers || []
			this.data.allowedRoles = x.allowedRoles || []
			this.dataLoaded = true
		})
	}
	
    public submitUpdatedData() {
        this.http.post<ApiPostResponse>(App.basePath + "/account/updateAccountValidators", $.extend({}, this.data, {allowedUsers: this.data.allowedUsers.map(m => m.id), allowedRoles: this.data.allowedRoles.map(m => m.id)})).subscribe(x => {
            if(x.code == 200) {
                Snackbar.show("success", "Configuration Updated")
				this.dataChanged = false
            }
        })
    }
}