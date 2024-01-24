import { Component, Input } from '@angular/core';
import {PaginatedBill} from "./bill.model";
import * as moment from "moment";
import { PaymentRequestHop } from '../payment-approval/payment.request.hop.model';
import { Util } from '../../util';
import { User } from '../user-management/user.model';
import {ModalDirective} from "ngx-bootstrap/modal";

@Component({
  selector: 'request-history',
  templateUrl: 'payment-request-approval-history.component.html',
  styleUrls: ['../payment-approval/hop-execution-panel.scss', './approver-steps.scss']
})
export class PRApprovalHistoryComponent {
   
   
  @Input("bills") public bills: PaginatedBill;
  @Input("hops") public hops: PaymentRequestHop[]
  public moment: any = moment
  public _u: any = Util
  public collapse = true
  public current_step_executors: User[]

  constructor() {

  }

  
  public showPendingAtUsers(users: User[], modal: ModalDirective) {
	this.current_step_executors = users
	modal.show()
  }
}