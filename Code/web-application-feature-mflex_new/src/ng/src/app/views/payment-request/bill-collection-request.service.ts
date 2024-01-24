import {EventEmitter, Injectable, Output} from '@angular/core';
import {App as app} from "../../app.model";
import {HttpClient} from "@angular/common/http";
import {PaginatedBillDetailTask} from "./bill-collection-request.model";
import {PaginatorComponent} from "../../generic/components/paginator.component";

@Injectable({
  providedIn: 'root'
})
export class BillCollectionRequestService {
  @Output() load: EventEmitter<PaginatedBillDetailTask> = new EventEmitter();

  public page: PaginatorComponent
  public beforeDataLoad: any

  constructor(private http: HttpClient) {
  }

  loadTableData() {
    if(this.beforeDataLoad) {
      this.beforeDataLoad()
    }
    this.http.post<PaginatedBillDetailTask>(app.basePath + "/paymentRequest/allBillCollectionRequest", {offset: this.page.offset, totalPerPage: this.page.currentPerPage}).subscribe(x => {
      this.load.emit(x)
    })
  }
}