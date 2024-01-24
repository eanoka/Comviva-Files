import {Component, EventEmitter, Input, OnChanges, OnInit, Output} from '@angular/core';

@Component({
    templateUrl: 'paginator.component.html',
    selector: 'paginator'
})
export class PaginatorComponent implements OnInit, OnChanges {
    @Input() public totalRecords: number = 0
    @Input() public offset: number = 0

    @Output() pageChange = new EventEmitter<any>();

    public perPageOptions: number[] = [10, 20, 50, 100];
    public currentPerPage: number = 10;
    public numberOfVisiblePage: number = 5;

    public totalNumberOfPage: number = 100;
    public currentPage: number = 50;
    public startPage: number = 48;
    public endPage: number = 52;

    ngOnInit() {
        this.calculateValues()
    }

    ngOnChanges() {
        this.calculateValues()
    }

    calculateValues() {
        this.totalNumberOfPage = Math.ceil(this.totalRecords / this.currentPerPage) || 1;
        this.currentPage = Math.ceil((this.offset + 1) / this.currentPerPage);
        if(this.currentPage > this.totalNumberOfPage) {
            this.currentPage = this.totalNumberOfPage;
        }
        this.offset = (this.currentPage - 1) * this.currentPerPage
        this.startPage = this.currentPage - Math.floor(this.numberOfVisiblePage / 2)
        if(this.startPage < 1) {
            this.startPage = 1
        }
        this.endPage = this.startPage + this.numberOfVisiblePage - 1
        if(this.endPage > this.totalNumberOfPage) {
            this.startPage -= this.endPage - this.totalNumberOfPage
            if(this.startPage < 1) {
                this.startPage = 1
            }
            this.endPage = this.totalNumberOfPage
        }
    }

    changePerPage(perPage: number) {
        this.currentPerPage = perPage
        this.calculateValues()
        this.pageChange.emit(this)
    }

    toPage(pageNumber: number) {
        this.currentPage = pageNumber
        this.offset = (this.currentPage - 1) * this.currentPerPage
        if(pageNumber < this.startPage) {
            this.startPage = pageNumber
            this.endPage = this.startPage + this.numberOfVisiblePage - 1
        } else if(pageNumber > this.endPage) {
            this.endPage = pageNumber
            this.startPage = this.endPage - this.numberOfVisiblePage + 1
        }
        this.pageChange.emit(this)
    }

    showBackwardPages() {
        if(this.startPage > 1) {
            this.startPage = this.startPage - this.numberOfVisiblePage
        }
        if(this.startPage < 1) {
            this.startPage = 1;
        }
        this.endPage = this.startPage + this.numberOfVisiblePage - 1
        if(this.endPage > this.totalNumberOfPage) {
            this.endPage = this.totalNumberOfPage
        }
    }

    showForwardPages() {
        if(this.endPage < this.totalNumberOfPage) {
            this.endPage = this.endPage + this.numberOfVisiblePage
        }
        if(this.endPage > this.totalNumberOfPage) {
            this.endPage = this.totalNumberOfPage
        }
        this.startPage = this.endPage - this.numberOfVisiblePage + 1
        if(this.startPage < 1) {
            this.startPage = 1;
        }
    }
}