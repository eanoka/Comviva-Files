import { Component } from '@angular/core';
import {Router} from "@angular/router";
import {FormHandlerUtil} from "../../generic/form-handler.util";
import {Client} from "../manage-bill-data/client.model";
import {Category} from "../manage-bill-data/category.model";
import {Company} from "../manage-bill-data/company.model";
import {App} from "../../app.model";
import {SessionUser} from "../user-management/session-user.model";
import * as $ from 'jquery';
import * as moment from 'moment';
import {PaginatedRequestDetail} from "./paginated-request-detail.model";
import {CreatePaymentRequestComponent} from "../payment-request/create.component";
import {Util} from "../../util";
import {PaginatorComponent} from "../../generic/components/paginator.component";
import {Actions} from "../../generic/actions.consts";
import {FilterPageData} from "./filter-page-data.model";

@Component({
  selector: 'bordered-content-wrapper',
  templateUrl: 'request-status.component.html',
  styleUrls: ['../payment-approval/hop-execution-panel.scss']
})
export class RequestStatusComponent extends FormHandlerUtil<Client[] | FilterPageData> {
  public accno: string
  public start: any
  public end: any
  public nullReference: any = null
  public subAccount: string[] = []
  public categories: Category[]
  public account: any = this.nullReference
  public category: Category = this.nullReference
  public company: Company = this.nullReference
  public loggedUser: SessionUser = App.user
  public newRequest:CreatePaymentRequestComponent
  public App: any = App
  public clients: Client[]
  public requests: PaginatedRequestDetail = null
  public _m: any = moment
  public _u: any = Util
  public lastCacheSubmitData: any = {}
  public _actions: any = Actions
  public divisionEntries: any[] = []
  public pdfGenerating: boolean = false
  public downloadingRequestId: number

  constructor(public router: Router) {
    super(App.user.isGP ? "/account/getAllActive" : "/billData/filterPageData", "/paymentRequest/getFilteredRequests")

    let state = router.getCurrentNavigation()?.extras?.state
    if(state) {
      this.onSubmit({valid: true, value: {start: state.start, my: state.my}})
    }

    this.submit.subscribe(x => {
      this.requests = x
      x.records.forEach(y => {
        if(y.creationTime) {
          y.creationTime = Util.toDate(y.creationTime)
        }
      })
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
    if(v.category){
	  x.category = v.category.id
	}else{
		x.category = null
	}
	if(v.company){
	  x.company = v.company.id
	}else{
		x.company = null
	}
	return this.lastCacheSubmitData = $.extend({}, v, x)
  }

  public loadTableData(page: PaginatorComponent) {
    this.onSubmit({valid: true, value: $.extend(this.lastCacheSubmitData, {offset: page.offset, totalPerPage: page.currentPerPage})})
  }

  protected onLoadData(x: Client[] | FilterPageData): void {
    if(x instanceof Array) {
      this.clients = x
    } else {
	  this.categories = x.categories
      this.divisionEntries = x.divisions
    }
  }
  
  public refineFilter() {
    this.lastCacheSubmitData = undefined
    this.requests = undefined
  }

  downPdf(ev, requestId: number) {
    ev.preventDefault()
    this.downloadingRequestId = requestId
    this.pdfGenerating = true
  }
  
  public onDownload() {
    this.pdfGenerating = false
  }
}