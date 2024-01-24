import { Component } from '@angular/core';
import {App} from "../../app.model";
import {HttpClient} from "@angular/common/http";
import * as moment from "moment"
import {SessionUser} from "../user-management/session-user.model";
import {Actions} from "../../generic/actions.consts";

@Component({
  templateUrl: 'approved-bill-pay-status.component.html',
  selector: 'hop-status',
  styleUrls: ["../payment-approval/hop-status.scss"]
})
export class ApprovedBillPayStatusComponent {
  public requestCount: number = 0;
  public monthsAgo: any = moment().subtract(1, "month").format("DD/MM/YYYY 00:00:00")
  public sessionUser: SessionUser = App.user;
  public Actions: typeof Actions = Actions;

  constructor(private http: HttpClient) {
    http.get<number>(App.basePath + "/paymentRequest/getApprovedCount").subscribe(value => this.requestCount = value)
  }
}