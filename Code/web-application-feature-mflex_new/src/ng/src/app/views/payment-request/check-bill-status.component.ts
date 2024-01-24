import { Component } from '@angular/core';
import {FormHandlerUtil} from "../../generic/form-handler.util";
import {SessionUser} from "../user-management/session-user.model";
import {App} from "../../app.model";
import {Category} from "../manage-bill-data/category.model";
import {Company} from "../manage-bill-data/company.model";
import {FilterPageData} from "./filter-page-data.model";
import {PaginatedBill} from "./bill.model";
import * as moment from "moment";
import {PaginatorComponent} from "../../generic/components/paginator.component";
import {Router} from "@angular/router";
import * as $ from 'jquery';
import {Client} from "../manage-bill-data/client.model";
import {Actions} from "../../generic/actions.consts";
import {Util} from "../../util";
import { User } from '../user-management/user.model';
import {ModalDirective} from "ngx-bootstrap/modal";

@Component({
  selector: 'bordered-content-wrapper',
  templateUrl: 'check-bill-status.component.html',
  styleUrls: ['../payment-approval/hop-execution-panel.scss', './approver-steps.scss']
})
export class CheckBillStatusComponent extends FormHandlerUtil<FilterPageData> {
  public accno: string
  public nullReference: any = null
  public loggedUser: SessionUser = App.user
  public categories: Category[]
  public subAccount: any[] = []
  public clients: Client[]
  public account: any = this.nullReference
  public category: Category = this.nullReference
  public company: Company = this.nullReference
  public divisionEntries: any[] = []
  public bills: any;
  public lastCacheSubmitData: any = {}
  public Actions: any = Actions
  public moment: any = moment
  public _u: any = Util
  public collapse = true
  public current_step_executors: User[]

  constructor(private router: Router) {
    super("/billData/filterPageData", "/billData/getFilteredBills")

    let state = router.getCurrentNavigation()?.extras?.state
    if(state) {
      this.fetchBillsByRequestId(0, 10, state.request)
    }

    this.submit.subscribe(this.onBillFetch.bind(this))
  }

  protected prepareSubmissionData(v: any): any {
    return this.lastCacheSubmitData = $.extend({}, v, {category: v.category ? v.category.id : null, company: v.company ? v.company.id : null, subAccount: this.subAccount.map(a => a.id)});
  }

  private onBillFetch(value: PaginatedBill) {
    value.records.forEach(y => {
      if(y.dueDate) {
        y.dueDate = moment(y.dueDate, "DD/MM/YYYY HH:mm:ss").toDate()
      }
      if(y.syncDate) {
        y.syncDate = moment(y.syncDate, "DD/MM/YYYY HH:mm:ss").toDate()
      }
    })
    this.bills = value
    this.submitting = false
  }

  private fetchBillsByRequestId(offset: number, perPage: number, requestId: number) {
    this.submitting = true
    this.http.post<PaginatedBill>(App.basePath + "/paymentRequest/getPaginatedBillsForRequest", this.lastCacheSubmitData = {offset: offset, totalPerPage: perPage, requestId: requestId}).subscribe(this.onBillFetch.bind(this), error => {
      this.submitting = false
    })
  }

  public onChangePagination(page: PaginatorComponent) {
    if(this.lastCacheSubmitData.requestId) {
      this.fetchBillsByRequestId(page.offset, page.currentPerPage, this.lastCacheSubmitData.requestId);
    } else {
      this.onSubmit({valid: true, value: $.extend(this.lastCacheSubmitData, {offset: page.offset, totalPerPage: page.currentPerPage})})
    }
  }

  protected onLoadData(x: FilterPageData): void {
    this.divisionEntries = x.divisions
    this.categories = x.categories
    this.clients = x.clients
  }
  
  public showPendingAtUsers(users: User[], modal: ModalDirective) {
	this.current_step_executors = users
	modal.show()
  }
  
  public refineFilter() {
    this.lastCacheSubmitData = undefined
    this.bills = undefined
  }
}