import {Component, HostBinding} from '@angular/core';
import {PaginatedBill} from "../payment-request/bill.model";
import {RequestSummary} from "../payment-approval/request.summary.model";
import {HttpClient} from "@angular/common/http";
import {App} from "../../app.model";
import {PaginatorComponent} from "../../generic/components/paginator.component";
import {Snackbar} from "../blocks/snackbar.component";
import {ApiPostResponse} from "../../generic/api-post-response.model";
import {Router} from "@angular/router";
import {ModalDirective} from "ngx-bootstrap/modal";
import {Actions} from "../../generic/actions.consts";
import {Util} from "../../util";
import {SessionUser} from "../user-management/session-user.model";
import * as moment from "moment"

@Component({
  templateUrl: 'bill-payment.component.html',
  styleUrls: ["../payment-approval/hop-execution-panel.scss"],
  selector: "hop-execution-panel"
})
export class BillPaymentComponent {
  public currentRequest: RequestSummary
  public currentSelectedRequest: RequestSummary
  public currentRequestId: string
  public totalAmountForCurrentRequest: number
  public selectedTotalAmount: number = 0
  public bills: PaginatedBill
  public requests: RequestSummary[] = []
  public _actions: any = Actions
  public pdfGenerating: boolean

  public selectedRequests: number[] = []
  public loggedUser: SessionUser = App.user

  public _moment: any = moment
  public _u: any = Util

  @HostBinding("class.busy")
  public requestsDataNotReady: boolean = true

  public tableDataLoading: boolean = false
  public finalizeAction: string
  public actionComment: string
  public pin: string
  
  public selectedBillIdsForRejectionInPayment: number[] = []

  constructor(private http: HttpClient, private router: Router) {
    http.post<RequestSummary[]>(App.basePath + "/paymentRequest/getApprovedRequests", {offset: 0, totalPerPage: 10}).subscribe(value => {
      this.requests = value
      this.requestsDataNotReady = false
    })
  }

  public showDetails(req: RequestSummary) {
    if(this.loggedUser.permissions.includes(Actions.VIEW_BILL_STATUS) && Util.toPRId(req.id) != this.currentRequestId) {
      this.currentRequest = req
      this.totalAmountForCurrentRequest = req.payableAmount
      this.fetchTableData(0, 10, req.id)
    }
  }
  
  public changeChildData($event: any){
	 console.log("test change child data: " + $event)
	 this.selectedBillIdsForRejectionInPayment = $event
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
    }, error => {
      this.tableDataLoading = false
    })
  }

  public onChangePagination(page: PaginatorComponent) {
    this.fetchTableData(page.offset, page.currentPerPage, parseInt(this.currentRequestId.substring(2), 16));
  }

  public onRequestSelectionChange(ev, req: RequestSummary, modal:ModalDirective) {
    if(ev.target.checked) {
      this.selectedTotalAmount += req.payableAmount
      this.selectedRequests.push(req.id)
      this.currentSelectedRequest = req
      if(this.selectedRequests.length > 1 && this.selectedBillIdsForRejectionInPayment.length >= 1){
	    modal.show()
	  }
    } else {
      this.selectedTotalAmount -= req.payableAmount
      this.selectedRequests.splice(this.selectedRequests.indexOf(req.id), 1)
      this.selectedBillIdsForRejectionInPayment.forEach(id => document.getElementById('rowid-'+id).style.backgroundColor = 'white')
      this.selectedBillIdsForRejectionInPayment = []
    }
  }

  public preChangeCheckBox(ok: boolean, modal: ModalDirective){
	if (ok){
      this.selectedBillIdsForRejectionInPayment.forEach(id => document.getElementById('rowid-'+id).style.backgroundColor = 'white')
      this.selectedBillIdsForRejectionInPayment = []
      modal.hide();
	} else {
	  this.selectedTotalAmount -= this.currentSelectedRequest.payableAmount
      this.selectedRequests.splice(this.selectedRequests.indexOf(this.currentSelectedRequest.id), 1)
	  var element = <HTMLInputElement> document.getElementById("checkid-" + this.currentSelectedRequest.id);
	  element.checked = false;
	  modal.hide();
	}
  }
  
  public isApproveDisabled():boolean{
  	return (this.selectedTotalAmount == 0) || ((this.selectedBillIdsForRejectionInPayment.length !== 0) && (this.selectedRequests.length == 1))
  }
  
  public preFinalize(isAllowed: boolean, modal: ModalDirective) {
    if(this.selectedRequests.length == 0) {
      Snackbar.show("danger", "No Request is Selected")
    }
    this.finalizeAction = isAllowed ? "Pay" : "Reject"
    modal.show()
  }

  public finalize(action: string, modal: ModalDirective) {
    modal.hide()
    this.http.post<ApiPostResponse>(App.basePath + "/paymentRequest/" + (action == "Pay" ? "initiatePayments" : "rejectApprovals"), {comment: this.actionComment, ids: this.selectedRequests, billIdsToReject: this.selectedBillIdsForRejectionInPayment, pin: this.pin}).subscribe(x => {
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
  
  public onClickShowDetail(req: RequestSummary, modal: ModalDirective) {
		if(this.currentSelectedRequest == null && this.currentSelectedRequest == req)
		{
			this.showDetails(req);
		}
		else if(this.currentSelectedRequest && this.selectedBillIdsForRejectionInPayment.length > 0)
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
			this.selectedBillIdsForRejectionInPayment.forEach(id => document.getElementById('rowid-' + id).style.backgroundColor = 'white')
			this.selectedBillIdsForRejectionInPayment = []
			modal.hide();
			this.showDetails(this.currentSelectedRequest)
		} else {
			modal.hide();
		}
	}
}