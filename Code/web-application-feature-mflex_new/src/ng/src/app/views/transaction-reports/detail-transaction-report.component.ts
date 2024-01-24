import { Component } from '@angular/core';
import {FormHandlerUtil} from "../../generic/form-handler.util";
import {Client} from "../manage-bill-data/client.model";
import {SessionUser} from "../user-management/session-user.model";
import {App} from "../../app.model";
import {Category} from "../manage-bill-data/category.model";
import {Company} from "../manage-bill-data/company.model";
import {FilterData} from "./filter-data.model";
import * as moment from "moment";
import * as $ from "jquery";
import {PaginatorComponent} from "../../generic/components/paginator.component";

@Component({
  selector: 'bordered-content-wrapper',
  templateUrl: 'detail-transaction-report.component.html'
})
export class DetailTransactionReportComponent extends FormHandlerUtil<FilterData> {
  public start: any
  public end: any
  public type: string = "detail"
  public lastCacheSubmitData: any
  public nullReference: any = null
  public account: any = this.nullReference
  public loggedUser: SessionUser = App.user
  public clients: Client[]
  public categories: Category[]
  public category: Category = this.nullReference
  public company: Company = this.nullReference
  public records: any[]
  public count: number = 0;
  public offset: number = 0;
  public moment: any = moment;
  public App: App = App
  public $: any = $

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
    this.categories = x.categories
  }

  public refineFilter() {
    this.lastCacheSubmitData = undefined
    this.records = undefined
  }

  public onChangePagination(page: PaginatorComponent) {
    this.onSubmit({valid: true, value: $.extend(this.lastCacheSubmitData, {offset: page.offset, perPage: page.currentPerPage})})
  }
}