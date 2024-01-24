import {Component, Input, Output, EventEmitter} from '@angular/core';
import {PaginatorComponent} from "../../generic/components/paginator.component";
import {PaginatedBill} from "../payment-request/bill.model";
import * as moment from "moment"
import {App} from "../../app.model";
import {SessionUser} from "../user-management/session-user.model";
import { Alert } from '../blocks/modal-confirm.component';

@Component({
    selector: 'bill-table',
    templateUrl: 'bill-table.component.html',
})
export class BillTableComponent {
    @Input("onChangePagination") onChangePagination: (page: PaginatorComponent) => void
    @Input("totalAmount") totalAmount: number
    @Input("bills") bills: PaginatedBill
    @Input("pdf-download") pdf: boolean
    @Input("xls-link") xls: string
    @Input("download-file") file: string
    @Input("requestId") requestId : number
    @Input("showTxnId") showTxnId : boolean
    @Input("paymentReqId") paymentReqId: number
    @Input("attachment") attachment: string
    @Input("selectedBillIdsForRejection") public selectedBillIdsForRejection: number[] = []
    @Input("selectedBillIdsForRejectionInPayment") public selectedBillIdsForRejectionInPayment: number[] = []
    @Input("selectedRequests") selectedRequests: number[] = []
    
    @Output() selelectBillIdsChange = new EventEmitter<number[]>();
	public outPutSelectedBillIds() {
		this.selelectBillIdsChange.emit(this.selectedBillIdsForRejection);
	}
	
	@Output() selelectBillIdsChangeInPayment = new EventEmitter<number[]>();
	public outPutSelectedBillIdsInPayment() {
		this.selelectBillIdsChangeInPayment.emit(this.selectedBillIdsForRejectionInPayment);
	}

    public _moment: any = moment

    public pdfGenerating: boolean
    public generatingText: string
    public loggedUser: SessionUser = App.user
	public App: any = App
  	
    constructor() {
    }

    downPdf() {
        this.pdfGenerating = true
    }

    public onDownload() {
        this.pdfGenerating = false
    }
    
    public showError(message: string) {
    	Alert.display("Bill Payment Error!!", message, 
    	{
           	buttons: {cancel: false, ok: true}
    	})
  	}
  	
  	public preSelect(id: number): boolean {
  		if(this.selectedBillIdsForRejection.indexOf(id) === -1){
			return false;
		}
		else
		{
			return true;
		}
	}
  	
  	public selectBillIdForRejecting(id: number) {
		console.log("Values:",this.selectedBillIdsForRejection)
		if(this.selectedBillIdsForRejection.indexOf(id) === -1){
			this.selectedBillIdsForRejection.push(id);
			this.outPutSelectedBillIds();
			this.outPutSelectedBillIdsInPayment()
			document.getElementById('rowid-'+id).style.backgroundColor = 'salmon';
		}else{
			this.selectedBillIdsForRejection = this.selectedBillIdsForRejection.filter(obj => obj !== id);
			this.outPutSelectedBillIds()
			this.outPutSelectedBillIdsInPayment()
			document.getElementById('rowid-'+id).style.backgroundColor = 'white';
		}
  	}
  	
  	public isActionEnabled(): boolean {
		if(this.selectedRequests && this.bills.request && this.selectedRequests.includes(this.bills.request.id) && this.selectedRequests.length == 1)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
}