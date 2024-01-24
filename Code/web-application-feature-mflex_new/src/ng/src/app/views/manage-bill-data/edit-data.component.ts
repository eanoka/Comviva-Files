import {AfterViewInit, Component, ElementRef, HostBinding, ViewChild} from '@angular/core';
import {BillData} from "./bill-data.model";
import {Snackbar} from "../blocks/snackbar.component";
import $ from 'jquery';
import {App, App as app} from "../../app.model";
import {PaginatorComponent} from "../../generic/components/paginator.component";
import {FilterPageData} from "../payment-request/filter-page-data.model";
import {HttpClient} from "@angular/common/http";
import {AppInjector} from "../../app.module";
import {Category} from "../manage-bill-data/category.model";
import {Client} from "../manage-bill-data/client.model";
import {Company} from "../manage-bill-data/company.model";
import {PaginatedBillData} from "./bill-data.model";
import {ApiPostResponse} from "../../generic/api-post-response.model";

@Component({
  templateUrl: 'edit-data.component.html',
  selector: 'bordered-content-wrapper',
  styleUrls: ['edit-data.component.scss']
})
export class EditDataComponent implements AfterViewInit {
  public totalData: number = 0
  public offset: number = 0
  public records: BillData[]
  public categories: Category[] = []
  public subAccount: {id: number, name: string}[] = []
  public accno: string
  public permissions: string[] = App.user.permissions
  public divisionEntries: {id: number, name: string}[] = []
  public nullReference: any = null
  public category: Category = this.nullReference
  public company: Company = this.nullReference

  @HostBinding("class.busy")
  public tableDataLoading: boolean = false

  @ViewChild("tbody", {read: ElementRef}) tbody: ElementRef;
  @ViewChild(PaginatorComponent) page: PaginatorComponent;

  public http: HttpClient = AppInjector.get(HttpClient)

  constructor() {
	this.http.get<FilterPageData>(app.basePath + "/billData/filterPageData").subscribe(x => {
         this.divisionEntries = x.divisions
         x.categories.forEach(y => {this.categories.push(y)})
    })
  }

  ngAfterViewInit(): void {
    this.loadTableData()
  }

  public loadTableData() {
    this.tableDataLoading = true
    this.http.post<PaginatedBillData>(app.basePath + "/billData/allBillData", {offset: this.page.offset,
        totalPerPage: this.page.currentPerPage, subAccount: this.subAccount.map(v => v.id), category: this.category == null ? null : this.category.id,
        company: this.company == null ? null : this.company.id, consumerId: this.accno}).subscribe(x => {
          this.records = x.records
          this.offset = x.offset
          this.totalData = x.count
          this.page.currentPerPage = x.perPage
          this.page.offset = x.offset
          this.page.totalRecords = x.count
          this.tableDataLoading = false
        })
  }

  delete(deletables: string[], callback: any) {
      this.http.post<ApiPostResponse>(app.basePath + "/billData/delete", {ids: deletables}).subscribe(x => {
        Snackbar.show(x.code == 200 ? "success" : "warning", x.message);
        if(callback) {
          callback()
        }
      })
    }

  deleteSelecteds() {
    let deletables = []
    $(this.tbody.nativeElement).find(":checked").each((x, y) => {
      deletables.push($(y).attr("bill-data-id"))
    })
    if(deletables.length) {
      this.delete(deletables, () => {
        this.loadTableData()
      })
    } else {
      Snackbar.show("warning", "You have selected no bill data to delete");
    }
  }
}
