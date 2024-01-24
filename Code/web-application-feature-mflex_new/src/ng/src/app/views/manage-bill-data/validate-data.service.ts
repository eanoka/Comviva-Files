import {EventEmitter, Injectable, Output} from '@angular/core';
import {App as app} from "../../app.model";
import {HttpClient} from "@angular/common/http";
import {BillData, PaginatedBillData} from "./bill-data.model";
import {ApiPostResponse} from "../../generic/api-post-response.model";
import {Snackbar} from "../blocks/snackbar.component";
import {PaginatorComponent} from "../../generic/components/paginator.component";

@Injectable({
  providedIn: 'root'
})
export class ValidateDataService {
  @Output() load: EventEmitter<PaginatedBillData> = new EventEmitter();

  public page: PaginatorComponent
  public beforeDataLoad: any
  public billData: BillData

  constructor(private http: HttpClient) {
  }

  loadTableData() {
    if(this.beforeDataLoad) {
      this.beforeDataLoad()
    }
    this.http.post<PaginatedBillData>(app.basePath + "/billData/billDataListForValidation", {offset: this.page.offset, totalPerPage: this.page.currentPerPage}).subscribe(x => {
      this.load.emit(x)
    })
  }

  approveBills(selectedIds: number[], comment: string, callback: any) {
    this.http.post<ApiPostResponse>(app.basePath + "/billData/approveBills", {selectedIds: selectedIds, comment: comment}).subscribe(x => {
      	Snackbar.show(x.code == 200 ? "success" : "warning", x.message);
    })
    if(callback) {
      	callback()
    }
  }
  
  rejectBills(selectedIds: number[], comment: string, callback: any) {
	if(typeof comment!='undefined' && comment && comment !== ""){
    	this.http.post<ApiPostResponse>(app.basePath + "/billData/rejectBills", {selectedIds: selectedIds, comment: comment}).subscribe(x => {
      	Snackbar.show(x.code == 200 ? "success" : "warning", x.message);
    })
    if(callback) {
      	callback()
    }
  	}else{
		Snackbar.show("warning", "Comment is required for rejecting BillData.");
	}
  }
  	public getWas(billdataId: number){
		 this.http.get<BillData>(app.basePath + "/billData/getDetail?id=" + billdataId).subscribe(x => {
			this.billData = x
		})
		return this.billData
	}
}