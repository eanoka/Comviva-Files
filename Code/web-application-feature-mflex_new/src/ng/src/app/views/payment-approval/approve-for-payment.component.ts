import {Component, HostBinding} from '@angular/core';
import {PaginatedBill} from "../payment-request/bill.model";
import * as moment from "moment"
import {App} from "../../app.model";
import {HttpClient} from "@angular/common/http";
import {RequestSummary} from "./request.summary.model";
import {PaginatorComponent} from "../../generic/components/paginator.component";
import {Snackbar} from "../blocks/snackbar.component";
import {ApiPostResponse} from "../../generic/api-post-response.model";
import {Router} from "@angular/router";
import {ModalDirective} from "ngx-bootstrap/modal";
import {Actions} from "../../generic/actions.consts";
import {Util} from "../../util";
import {SessionUser} from "../user-management/session-user.model";

@Component({
  templateUrl: 'approve-for-payment.component.html',
  styleUrls: ["hop-execution-panel.scss"],
  selector: "hop-execution-panel"
})
export class ApproveForPaymentComponent{
  public currentRequest: RequestSummary
  public currentRequestId: string
  public totalAmountForCurrentRequest: number
  public selectedTotalAmount: number = 0
  public bills: PaginatedBill
  public selectedBillIdsForRejection: number[] = []
  public requests: RequestSummary[] = []
  public _actions: any = Actions
  public pdfGenerating: boolean

  public selectedRequests: number[] = []
  public loggedUser: SessionUser = App.user

  public moment: any = moment
  public _u: any = Util
  public App: any = App
  
  @HostBinding("class.busy")
  public requestsDataNotReady: boolean = true

  public tableDataLoading: boolean = false
  public finalizeAction: string
  public actionComment: string
  public attachmentForcurrentRequest :string
  public originalRequestId: number
  
  public currentSelectedRequest: RequestSummary

  constructor(private http: HttpClient, private router: Router) {
    http.post<RequestSummary[]>(App.basePath + "/paymentRequest/getApprovalWaitingRequests", {offset: 0, totalPerPage: 10}).subscribe(value => {
      this.requests = value
      value.forEach(v => {
        v.date = moment(v.date, "DD/MM/YYYY HH:mm:ss").toDate()
      })
      this.requestsDataNotReady = false
    })
  }

  public showDetails(req: RequestSummary) {
    if(this.loggedUser.permissions.includes(Actions.VIEW_BILL_STATUS) && Util.toPRId(req.id) != this.currentRequestId) {
      this.totalAmountForCurrentRequest = req.payableAmount
      this.originalRequestId = req.id
      this.currentRequest = req
      this.fetchTableData(0, 10, req.id)
    }
  }

  private fetchTableData(offset: number, perPage: number, requestId: number) {
    this.tableDataLoading = true
    this.http.post<PaginatedBill>(App.basePath + "/paymentRequest/getPaginatedBillsForRequest", {offset: offset, totalPerPage: perPage, requestId: requestId}).subscribe(value => {
      this.currentRequestId = Util.toPRId(requestId)
      if(this.currentRequest.date && typeof this.currentRequest.date === "string") {
        this.currentRequest.date = moment(this.currentRequest.date, "DD/MM/YYYY HH:mm:ss").toDate()
      }
      value.records.forEach(y => {
        if(y.dueDate) {
          y.dueDate = moment(y.dueDate, "DD/MM/YYYY HH:mm:ss").toDate()
        }
        if(y.syncDate) {
          y.syncDate = moment(y.syncDate, "DD/MM/YYYY HH:mm:ss").toDate()
        }
      })
      this.bills = value
      this.tableDataLoading = false
      this.attachmentForcurrentRequest = value.request.attachment
    }, error => {
      this.tableDataLoading = false
    })
  }

  public onChangePagination(page: PaginatorComponent) {
    this.fetchTableData(page.offset, page.currentPerPage, parseInt(this.currentRequestId.substring(2), 16));
  }
  
  public changeChildData($event: any){
	 this.selectedBillIdsForRejection = $event
  }

  public onRequestSelectionChange(ev, req: RequestSummary, modal: ModalDirective) {
	this.currentSelectedRequest = req
    if(ev.target.checked) {
	  this.currentSelectedRequest = req
	  this.selectedTotalAmount += this.currentSelectedRequest.payableAmount
      this.selectedRequests.push(this.currentSelectedRequest.id)
      if(this.selectedRequests.length > 1 && this.selectedBillIdsForRejection.length >= 1){
	    modal.show()
	  }
    } else {
      this.selectedTotalAmount -= req.payableAmount
      this.selectedRequests.splice(this.selectedRequests.indexOf(req.id), 1)
      this.selectedBillIdsForRejection.forEach(id => document.getElementById('rowid-'+id).style.backgroundColor = 'white')
      this.selectedBillIdsForRejection = []
    }
  }
  
  public preChangeCheckBox(ok: boolean, modal: ModalDirective){
	if (ok){
	  this.selectedBillIdsForRejection.forEach(id => document.getElementById('rowid-'+id).style.backgroundColor = 'white')
      this.selectedBillIdsForRejection = []
      modal.hide();
	} else {
	  this.selectedTotalAmount -= this.currentSelectedRequest.payableAmount
      this.selectedRequests.splice(this.selectedRequests.indexOf(this.currentSelectedRequest.id), 1)
	  var element = <HTMLInputElement> document.getElementById("checkid-" + this.currentSelectedRequest.id);
	  element.checked = false;
	  modal.hide();
	}
  }
  
	public onClickShowDetail(req: RequestSummary, modal: ModalDirective) {
		if(this.currentSelectedRequest == null && this.currentSelectedRequest == req)
		{
			this.showDetails(req);
		}
		else if(this.currentSelectedRequest && this.selectedBillIdsForRejection.length > 0)
		{
			this.currentSelectedRequest = req
			modal.show()			
		}
		else
		{
			this.showDetails(req);
		}
	}
	
	public preShowDetail(ok: boolean, modal: ModalDirective) {
		if (ok) {
			this.selectedBillIdsForRejection.forEach(id => document.getElementById('rowid-' + id).style.backgroundColor = 'white')
			this.selectedBillIdsForRejection = []
			modal.hide();
			this.showDetails(this.currentSelectedRequest)
		} else {
			modal.hide();
		}
	}
  
  public isApproveDisabled():boolean{
  	return (this.selectedTotalAmount == 0) || ((this.selectedBillIdsForRejection.length !== 0) && (this.selectedRequests.length == 1))
  }
  
  public preFinalize(isAllowed: boolean, modal: ModalDirective) {
    if(this.selectedRequests.length == 0) {
      Snackbar.show("danger", "No Request is Selected")
    }
    this.finalizeAction = isAllowed ? "Approve" : "Reject"
    modal.show()
  }

  public finalize(action: string, modal: ModalDirective) {
    modal.hide()
    this.http.post<ApiPostResponse>(App.basePath + "/paymentRequest/" + (action == "Approve" ? "approvePayments" : "rejectApprovals"), {comment: this.actionComment, ids: this.selectedRequests, billIdsToReject: this.selectedBillIdsForRejection}).subscribe(x => {
      Snackbar.show(x.code == 200 ? "success" : "danger", x.message)
      if(x.code == 200) {
        if(App.user.permissions.includes(Actions.VIEW_REQUEST_STATUS)) {
          this.router.navigateByUrl("/payment-request/request-status", {state: {start: moment().subtract(1, 'month').format("DD/MM/YYYY 00:00:00")}})
        } else {
          this.router.navigateByUrl("/dashboard")
        }
      }
    })
  }

  downPdf() {
    if(!this.currentRequestId) {
      return
    }
    this.pdfGenerating = true
  }

  public onDownload() {
    this.pdfGenerating = false
  }
}