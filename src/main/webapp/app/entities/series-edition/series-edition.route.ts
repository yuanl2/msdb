import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes, CanActivate } from '@angular/router';

import { UserRouteAccessService } from '../../shared';
import { PaginationUtil } from 'ng-jhipster';

import { SeriesEditionComponent } from './series-edition.component';
import { SeriesEditionDetailComponent } from './series-edition-detail.component';
import { SeriesEditionPopupComponent } from './series-edition-dialog.component';
import { SeriesEditionDeletePopupComponent } from './series-edition-delete-dialog.component';
import { SeriesEditionCalendarPopupComponent } from './series-edition-calendar-dialog.component';

import { Principal } from '../../shared';

@Injectable()
export class SeriesEditionResolvePagingParams implements Resolve<any> {

  constructor(private paginationUtil: PaginationUtil) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
      let page = route.queryParams['page'] ? route.queryParams['page'] : '1';
      let sort = route.queryParams['sort'] ? route.queryParams['sort'] : 'id,asc';
      return {
          page: this.paginationUtil.parsePage(page),
          predicate: this.paginationUtil.parsePredicate(sort),
          ascending: this.paginationUtil.parseAscending(sort)
    };
  }
}

export const seriesEditionRoute: Routes = [
//  {
//    path: 'series-edition',
//    component: SeriesEditionComponent,
//    resolve: {
//      'pagingParams': SeriesEditionResolvePagingParams
//    },
//    data: {
//        authorities: ['ROLE_USER'],
//        pageTitle: 'motorsportsDatabaseApp.seriesEdition.home.title'
//    }
//  }, 
  {
    path: 'series/series-edition/:id',
    component: SeriesEditionDetailComponent,
    data: {
        authorities: ['ROLE_USER'],
        pageTitle: 'motorsportsDatabaseApp.series.seriesEdition.home.title'
    }
  }
];

export const seriesEditionPopupRoute: Routes = [
  {
    path: ':idSeries/series-edition-new',
    component: SeriesEditionPopupComponent,
    data: {
        authorities: ['ROLE_EDITOR', 'ROLE_ADMIN'],
        pageTitle: 'motorsportsDatabaseApp.series.seriesEdition.home.title'
    },
    outlet: 'popup'
  },
  {
    path: 'series-edition/:id/edit',
    component: SeriesEditionPopupComponent,
    data: {
        authorities: ['ROLE_EDITOR', 'ROLE_ADMIN'],
        pageTitle: 'motorsportsDatabaseApp.series.seriesEdition.home.title'
    },
    outlet: 'popup'
  },
  {
      path: ':id/calendar',
      component: SeriesEditionCalendarPopupComponent,
      data: {
          authorities: ['ROLE_EDITOR', 'ROLE_ADMIN'],
          pageTitle: 'motorsportsDatabaseApp.series.seriesEdition.home.title'
      },
      outlet: 'popup'
    },
  {
    path: 'series-edition/:id/delete',
    component: SeriesEditionDeletePopupComponent,
    data: {
        authorities: ['ROLE_ADMIN'],
        pageTitle: 'motorsportsDatabaseApp.series.seriesEdition.home.title'
    },
    outlet: 'popup'
  }
];
