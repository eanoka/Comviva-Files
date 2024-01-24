import { Component } from '@angular/core';
import {FormHandlerUtil} from "../../generic/form-handler.util";
import {FilterData} from "./filter-data.model";
import {SessionUser} from "../user-management/session-user.model";
import {App} from "../../app.model";
import {Client} from "../manage-bill-data/client.model";
import {Category} from "../manage-bill-data/category.model";
import {Company} from "../manage-bill-data/company.model";
import {PaginatorComponent} from "../../generic/components/paginator.component";
import * as $ from "jquery";
import * as moment from "moment";
import {Snackbar} from "../blocks/snackbar.component";

@Component({
  selector: 'bordered-content-wrapper',
  templateUrl: 'prepaid-token-enquiry.component.html'
})
export class PrepaidTokenEnquiryComponent extends FormHandlerUtil<FilterData> {
  public start: any
  public end: any
  public type: string = "prepaid"
  public accNo: string
  public lastCacheSubmitData: any
  public nullReference: any = null
  public account: any = this.nullReference
  public loggedUser: SessionUser = App.user
  public App: App = App
  public $: any = $
  public clients: Client[]
  public categories: Category[]
  public category: Category = this.nullReference
  public company: Company = this.nullReference
  public records: any[]
  public count: number = 0;
  public offset: number = 0;
  public moment: any = moment

  constructor() {
    super("/report/getFilterPageData", "/report/getDetailReportData")
    this.submit.subscribe(x => {
      this.records = x.records
      this.offset = x.offset
      this.count = x.total
    })
  }

  protected prepareSubmissionData(v: any): any {
    let x: any = {}
    if(v.start && v.start.length == 10) {
      x.start = moment(v.start, "YYYY-MM-DD").format("DD/MM/YYYY 00:00:00")
    }
    if(v.end && v.end.length == 10) {
      x.end = moment(v.end, "YYYY-MM-DD").format("DD/MM/YYYY 23:59:59")
    }
    if(v.category) {
      x.category = v.category.id
    }
    if(v.company) {
      x.company = v.company.id
    }
    return this.lastCacheSubmitData = $.extend({}, {offset: 0, perPage: 10}, this.lastCacheSubmitData, v, x);
  }

  protected onLoadData(x: FilterData): void {
    this.clients = x.clients
    this.categories = x.categories.filter((v, i, a) => {
      v.companies = v.companies.filter((w, j, b) => !w.hasBill)
      return v.companies.length > 0
    })
  }

  public refineFilter() {
    this.lastCacheSubmitData = undefined
    this.records = undefined
  }

  public onChangePagination(page: PaginatorComponent) {
    this.onSubmit({valid: true, value: $.extend(this.lastCacheSubmitData, {offset: page.offset, perPage: page.currentPerPage})})
  }

  public sendSms(ev: any) {
    let url = ev.target.getAttribute("href");
    this.http.get<any>(url).subscribe(x => {
      Snackbar.show(x.code == 200 ? "success" : "danger", x.message)
    });
    ev.preventDefault();
  }
}