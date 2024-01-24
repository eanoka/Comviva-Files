import {EventEmitter, Injectable, Output} from "@angular/core";
import {PaginatorComponent} from "../../generic/components/paginator.component";
import {HttpClient} from "@angular/common/http";
import {App as app} from "../../app.model";
import {PaginatedBill} from "./bill.model";

@Injectable({
    providedIn: 'root'
})
export class PaymentRequestService {
    @Output() load: EventEmitter<PaginatedBill> = new EventEmitter();

    public beforeDataLoad: any
    public dataLoadUrl

    constructor(private http: HttpClient) {
    }

    loadTableData(page: PaginatorComponent) {
        let payload = {offset: page.offset, totalPerPage: page.currentPerPage}
        if(this.beforeDataLoad) {
            this.beforeDataLoad(payload)
        }
        this.http.post<PaginatedBill>(app.basePath + this.dataLoadUrl, payload).subscribe(x => {
            this.load.emit(x)
        })
    }
}