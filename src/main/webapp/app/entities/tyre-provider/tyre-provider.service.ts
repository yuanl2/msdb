import { Injectable } from '@angular/core';
import { Http, Response } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { TyreProvider } from './tyre-provider.model';
import { ResponseWrapper, createRequestOption } from '../../shared';

@Injectable()
export class TyreProviderService {

    private resourceUrl = 'api/tyre-providers';
    private resourceSearchUrl = 'api/_search/tyre-providers';
    private typeaheadSearchUrl= 'api/_typeahead/tyres';

    constructor(private http: Http) { }

    create(tyreProvider: TyreProvider): Observable<TyreProvider> {
        const copy = this.convert(tyreProvider);
        return this.http.post(this.resourceUrl, copy).map((res: Response) => {
            return res.json();
        });
    }

    update(tyreProvider: TyreProvider): Observable<TyreProvider> {
        const copy = this.convert(tyreProvider);
        return this.http.put(this.resourceUrl, copy).map((res: Response) => {
            return res.json();
        });
    }

    find(id: number): Observable<TyreProvider> {
        return this.http.get(`${this.resourceUrl}/${id}`).map((res: Response) => {
            return res.json();
        });
    }

    query(req?: any): Observable<ResponseWrapper> {
        const options = createRequestOption(req);
        return this.http.get(this.resourceUrl, options)
            .map((res: Response) => this.convertResponse(res));
    }
    
    typeahead(req?: string): Observable<ResponseWrapper> {
        return this.http.get(`${this.typeaheadSearchUrl}?query=${req}`)
            .map((res: Response) => this.convertResponse(res));
    }

    delete(id: number): Observable<Response> {
        return this.http.delete(`${this.resourceUrl}/${id}`);
    }

    search(req?: any): Observable<ResponseWrapper> {
        const options = createRequestOption(req);
        return this.http.get(this.resourceSearchUrl, options)
            .map((res: any) => this.convertResponse(res));
    }

    private convertResponse(res: Response): ResponseWrapper {
        const jsonResponse = res.json();
        return new ResponseWrapper(res.headers, jsonResponse, res.status);
    }

    private convert(tyreProvider: TyreProvider): TyreProvider {
        const copy: TyreProvider = Object.assign({}, tyreProvider);
        return copy;
    }
}
