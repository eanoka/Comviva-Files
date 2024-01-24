import {Component, HostBinding} from '@angular/core';
import {Role} from "./role.model";
import {HttpClient} from "@angular/common/http";
import {PaginatedRole} from "./paginated-role.model";
import {App as app} from "../../app.model";
import {PaginatorComponent} from "../../generic/components/paginator.component";

@Component({
  selector: 'bordered-content-wrapper',
  templateUrl: 'role-list.component.html'
})
export class RoleListComponent {
  public totalData: number = 0
  public offset: number = 0
  public records: Role[]

  @HostBinding("class.busy")
  public tableDataLoading: boolean = false

  constructor(private http: HttpClient) {
    this.loadData(null)
  }

  public loadData(page: PaginatorComponent) {
    this.tableDataLoading = true
    this.http.post<PaginatedRole>(app.basePath + "/role/listableRoles", {offset: page ? page.offset : this.offset, totalPerPage: page ? page.currentPerPage : 10}).subscribe(x => {
      this.totalData = x.count
      this.offset = x.offset
      this.records = x.records
      this.tableDataLoading = false
    })
  }
}