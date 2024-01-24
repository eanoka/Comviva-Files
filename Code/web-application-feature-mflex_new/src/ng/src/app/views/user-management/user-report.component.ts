import {Component, HostBinding} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {PaginatorComponent} from "../../generic/components/paginator.component";
import {App, App as app} from "../../app.model";
import {PaginatedUser} from "./paginated-user.model";
import {User} from "./user.model";
import {jsPDF} from "jspdf"
import * as $ from "jquery"

@Component({
  templateUrl: 'user-report.component.html'
})
export class UserReportComponent {
  public totalData: number = 0
  public offset: number = 0
  public records: User[]

  public pdfGenerating: boolean
  public generatingText: string

  @HostBinding("class.busy")
  public tableDataLoading: boolean = false

  constructor(private http: HttpClient) {
    this.loadData(null)
  }

  public loadData(page: PaginatorComponent) {
    this.tableDataLoading = true
    this.http.post<PaginatedUser>(app.basePath + "/user/listableUsers", {offset: page ? page.offset : this.offset, totalPerPage: page ? page.currentPerPage : 10}).subscribe(x => {
      this.totalData = x.count
      this.offset = x.offset
      this.records = x.records
      this.tableDataLoading = false
    })
  }

  public downPdf() {
    this.pdfGenerating = true
    this.generatingText = "Generating PDF "
    let interval = setInterval(() => {
      if(this.generatingText.length == 25) {
        this.generatingText = "Generating PDF "
        return
      }
      this.generatingText += "."
    }, 1000)

    this.http.post<PaginatedUser>(App.basePath + "/user/listableUsers", {offset: 0, totalPerPage: -1}).subscribe(value => {
      let pdfPadding = 5 //mm
      let pageHeight = 297 //mm
      setTimeout(() => {
        let container = $("#pdf-template")
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

        const doc = new jsPDF();
        doc.html($("#pdf-template")[0], {
          html2canvas: {
            scale: 0.2648 //based on experience
          },
          callback: (doc) => {
            doc.save("User List.pdf");
            this.pdfGenerating = false
            clearInterval(interval)
          }
        });
      })
    }, error => {
      this.pdfGenerating = false
      clearInterval(interval)
    })
  }
}
