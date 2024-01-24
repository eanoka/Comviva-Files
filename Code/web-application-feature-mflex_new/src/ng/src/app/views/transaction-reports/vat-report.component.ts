import { Component } from '@angular/core';
import {FormHandlerUtil} from "../../generic/form-handler.util";
import {App} from "../../app.model";
import {Company} from "../manage-bill-data/company.model";
import * as moment from "moment";
import * as $ from "jquery";
import {PaginatorComponent} from "../../generic/components/paginator.component";
import { FilterVatData } from './filter-vat-data.model';
import { Util } from '../../util';
import { RequestModal } from "./request-data.model";

@Component({
  selector: 'bordered-content-wrapper',
  templateUrl: 'vat-report.component.html'
})
export class VatReportComponent extends FormHandlerUtil<FilterVatData> {
  
  public nullReference: any = null
  public lastCacheSubmitData: any
  public company: Company = this.nullReference
  public records: any[]
  public count: number = 0;
  public offset: number = 0;
  public moment: any = moment;
  public App: App = App
  public $: any = $
  public _u: any = Util
  public requests: any[]
  public request: RequestModal = this.nullReference

  constructor() {
    super("/report/getFilterVatPageData", "/report/getVatReportData")
    this.submit.subscribe(x => {
      this.records = x.records
      this.offset = x.offset
      this.count = x.total
    })
  }

  protected prepareSubmissionData(v: any): any {
    let x: any = {}
    if(v.request) {
      x.requestId = v.request.id
    }
    if(v.company) {
      x.companyCode = v.company
    }
    return this.lastCacheSubmitData = $.extend({}, {offset: 0, perPage: 10, requestId: v.request.id, companyCode: v.company}, this.lastCacheSubmitData, v, x);
  }

  protected onLoadData(x: FilterVatData): void { 
	this.requests = x.requests
  }
  
  public refineFilter() {
    this.lastCacheSubmitData = undefined
    this.records = undefined
  }

  public onChangePagination(page: PaginatorComponent) {
    this.onSubmit({valid: true, value: $.extend(this.lastCacheSubmitData, {offset: page.offset, perPage: page.currentPerPage})})
  }
}