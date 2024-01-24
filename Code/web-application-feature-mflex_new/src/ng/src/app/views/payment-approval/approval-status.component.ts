import { Component } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {App} from "../../app.model";
import * as moment from "moment"
import {SessionUser} from "../user-management/session-user.model";
import {Actions} from "../../generic/actions.consts";

@Component({
  templateUrl: 'approval-status.component.html',
  selector: 'hop-status',
  styleUrls: ["hop-status.scss"]
})
export class ApprovalStatusComponent {
  public requestCount: number = 0;
  public monthsAgo: any = moment().subtract(1, "month").format("DD/MM/YYYY 00:00:00")
  public sessionUser: SessionUser = App.user;
  public Actions: typeof Actions = Actions;

  constructor(private http: HttpClient) {
    http.get<number>(App.basePath + "/paymentRequest/getApprovalWaitingCount").subscribe(value => this.requestCount = value)
  }
}