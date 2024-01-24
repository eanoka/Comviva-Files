import {AfterViewInit, Component, EventEmitter, Input, Output} from "@angular/core";
import {SessionUser} from "../user-management/session-user.model";
import {App} from "../../app.model";
import {RequestSummary} from "../payment-approval/request.summary.model";
import * as moment from "moment"
import {PaginatedBill} from "./bill.model";
import {jsPDF} from "jspdf"
import * as $ from "jquery"
import {HttpClient} from "@angular/common/http";
import {Util} from "../../util";

@Component({
    selector: 'request-pdf-download',
    templateUrl: 'request-pdf-generator.component.html'
})
export class RequestPdfDownloaderComponent implements AfterViewInit {
    public generatingText: string
    public loggedUser: SessionUser = App.user
    @Input("requestId") public requestId: number
    @Input("showTxnId") showTxnId : boolean
    public requestSummary: RequestSummary
    public requestStrId: string
    public moment: any = moment
    public bills: PaginatedBill
    @Output() onDownload = new EventEmitter<any>();

    constructor(private http: HttpClient) {}

    ngAfterViewInit(): void {
        this.generatingText = "Generating PDF "
        let interval = setInterval(() => {
            if(this.generatingText.length == 25) {
                this.generatingText = "Generating PDF "
                return
            }
            this.generatingText += "."
        }, 1000)

        this.requestStrId = Util.toPRId(this.requestId)
        this.http.post<PaginatedBill>(App.basePath + "/paymentRequest/getPaginatedBillsForRequest", {offset: 0, totalPerPage: -1, requestId: this.requestId}).subscribe(value => {
            value.records.forEach(y => {
                if(y.dueDate) {
                    y.dueDate = moment(y.dueDate, "DD/MM/YYYY HH:mm:ss").toDate()
                }
                if(y.syncDate) {
                    y.syncDate = moment(y.syncDate, "DD/MM/YYYY HH:mm:ss").toDate()
                }
            })
            this.bills = value
            this.requestSummary = this.bills.request
            this.requestSummary.date = moment(this.requestSummary.date, "DD/MM/YYYY HH:mm:ss").toDate()

            const doc = new jsPDF("p", "mm", "a4");
            let heightInPx = doc.internal.pageSize.getHeight();

            let pdfPadding = 5 //mm
            let pageHeight = 297 //mm
            setTimeout(() => {
                let container = $("#pdf-template")
                let heightInHtmlPx = container.outerHeight() + 2; // adding 2 pixels to have some scaling fix
                let scale = heightInPx / heightInHtmlPx

                let containerTop = container.offset().top
                let pxPerMM = parseFloat(container.css("padding-left")) / pdfPadding
                let headerRow = container.find("tr:eq(0)")
                headerRow.find("th").each(function() {
                    let cell = $(this)
                    let newCell = $("<td style='font-weight: bold'></td>")
                    newCell.html(cell.html())
                    cell.after(newCell).remove()
                })
                let cellCount = headerRow.find("th").length
                let seperatorRow = $("<tr><td colspan='" + cellCount + "'></td></tr>")
                let currentPage = 1
                container.find("tr").each(function() {
                    let row = $(this)
                    let rowOffset = row.offset();
                    let bottom = rowOffset.top + row.height();
                    if((bottom - containerTop) / pxPerMM + pdfPadding > currentPage * pageHeight) {
                        row.before(seperatorRow.clone());
                        row.before(headerRow.clone());
                        let newHeaderTop = (row.prev().offset().top - containerTop) / pxPerMM
                        let seperatorHeight = currentPage * pageHeight + pdfPadding - newHeaderTop
                        row.prev().prev().height(seperatorHeight + "mm")
                        currentPage++
                    }
                })

                console.log("generating pdf with scale " + scale)
                doc.html($("#pdf-template")[0], {
                    html2canvas: {
                        scale: scale
                    },
                    callback: (doc) => {
                        doc.save("Payment Request Details-" + this.requestStrId + ".pdf");
                        this.onDownload.emit()
                    }
                });
            })
        }, error => {
            this.onDownload.emit()
        })
    }
}