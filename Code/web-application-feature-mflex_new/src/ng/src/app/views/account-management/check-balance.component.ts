import {Component} from '@angular/core';
import {App} from "../../app.model";
import {Balance} from "./balance.model";
import {FormHandlerUtil} from "../../generic/form-handler.util";
import {Client} from "../manage-bill-data/client.model";
import * as moment from "moment";

@Component({
  templateUrl: 'check-balance.component.html',
  styleUrls: ['check-balance.component.scss'],
    selector: 'bordered-content-wrapper'
})
export class CheckBalanceComponent extends FormHandlerUtil<any> {
    public accounts: Client[]
    public accountLoaded: boolean = false
    public collecting: boolean = false
    public balance: Balance
    public app: any = App
    public mmnt: any = moment

    public nullReference: any = null

    public account: number = this.nullReference

    constructor() {
        super(App.user.isGP ? '/account/getAllActive' : '/account/checkBalance', '/account/checkBalance', false);
        if(App.user.isGP) {
            this.submit.subscribe(x => this.balance = x)
        }
    }

    protected onLoadData(x: any) {
        if(App.user.isGP) {
            this.accounts = x
            this.accountLoaded = true
        } else {
            this.balance = x
        }
    }
}
