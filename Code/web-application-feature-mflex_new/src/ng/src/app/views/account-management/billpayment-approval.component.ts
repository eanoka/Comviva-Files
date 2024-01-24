import {AfterViewInit, Component} from '@angular/core';
import {BillpaymentApproval, BillpaymentApprovalHop} from "./billpayment-approval.model";
import {HttpClient} from "@angular/common/http";
import {App} from "../../app.model";
import {WorkflowHop} from "./workflow-hop.model";
import {AllowDeny} from "../user-management/action.model";
import {User} from "../user-management/user.model";
import {Role} from "../role-management/role.model";
import {ApiPostResponse} from "../../generic/api-post-response.model";
import {Snackbar} from "../blocks/snackbar.component";
import * as $ from 'jquery';

@Component({
  templateUrl: 'billpayment-approval.component.html',
  styleUrls: ['billpayment-approval.component.scss'],
    selector: 'bordered-content-wrapper'
})
export class BillPaymentApprovalComponent implements AfterViewInit {
    public model: BillpaymentApproval = new BillpaymentApproval()
    public dataLoaded: boolean
    public modelChanged: boolean = false
    public allUsers: any[] = []
    public allRoles: any[] = []

    constructor(private http: HttpClient) {
    }

    ngAfterViewInit(): void {
        this.http.get<BillpaymentApproval>(App.basePath + "/account/getAccountPaymentApproverConfig").subscribe(x => {
            this.model = x
            this.dataLoaded = true
            this.allUsers = this.model.users
            this.allRoles = this.model.roles
        })
    }

    public moveUp(i: number) {
        let data = this.model.hops[i]
        this.model.hops[i] = this.model.hops[i-1]
        this.model.hops[i-1] = data
        this.modelChanged = true
    }

    public moveDown(i: number) {
        let data = this.model.hops[i]
        this.model.hops[i] = this.model.hops[i+1]
        this.model.hops[i+1] = data
        this.modelChanged = true
    }

    public removeIt(i: number) {
        this.model.hops.splice(i, 1)
        this.modelChanged = true
    }

    public addNew() {
        let newHop = new BillpaymentApprovalHop();
        this.model.hops.push(newHop)
        newHop.hop = new WorkflowHop()
        newHop.hop.requiredAction = new AllowDeny()
        this.modelChanged = true
    }

    removeProps(obj: any, ...args: string[]): any {
        args.map(a => delete obj[a])
        return obj
    }

    public submitUpdatedData() {
        this.http.post<ApiPostResponse>(App.basePath + "/account/saveAccountPaymentApproverConfig", this.removeProps($.extend({}, this.model, {firstLevelApprover: {boundUsers: this.model.firstLevelApprover.users.map(u => u.id), boundRoles: this.model.firstLevelApprover.roles.map(r => r.id)}, lastLevelApprover: {boundUsers: this.model.lastLevelApprover.users.map(u => u.id), boundRoles: this.model.lastLevelApprover.roles.map(r => r.id)}, hops: this.model.hops.map(h => this.removeProps($.extend({}, h, {boundUsers: h.users.map(u => u.id), boundRoles: h.roles.map(r => r.id)}), "users", "roles"))}), "users", "roles")).subscribe(x => {
            if(x.code == 200) {
                Snackbar.show("success", "Configuration Updated")
                this.modelChanged = false
            }
        })
    }
}