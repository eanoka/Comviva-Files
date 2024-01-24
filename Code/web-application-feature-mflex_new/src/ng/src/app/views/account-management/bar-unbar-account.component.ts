import { Component } from '@angular/core';
import {FormHandlerUtil} from "../../generic/form-handler.util";
import {Client} from "../manage-bill-data/client.model";
import {HttpClient} from "@angular/common/http";
import {App} from "../../app.model";

@Component({
  selector: 'bordered-content-wrapper',
  templateUrl: 'bar-unbar-account.component.html'
})
export class BarUnbarAccountComponent extends FormHandlerUtil<Client[]> {
  public accounts: Client[]
  public accountLoaded: boolean = false
  public collecting: boolean = false
  public bar: boolean

  public nullReference: any = null

  public account: number = this.nullReference
  public action: boolean

  constructor(private remote: HttpClient) {
    super('/account/getAll', '/account/barUnbarAccount', false);
  }

  protected onLoadData(x: Client[]) {
    this.accounts = x
    this.accountLoaded = true
  }

  public getBarDetail() {
    this.collecting = true
    this.remote.get<any>(App.basePath + "/account/isBarred?accountId=" + this.account).subscribe(x => {
      this.bar = x
      this.collecting = false
    }, y => {
      this.collecting = false
    })
  }
}