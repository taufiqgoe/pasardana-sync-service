CREATE TABLE IF NOT EXISTS public.stocks (
	code varchar NOT NULL,
	name varchar NOT NULL,
	CONSTRAINT stocks_pk PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.stock_report_property (
	id numeric NOT NULL,
	"name" text NULL,
	CONSTRAINT stock_report_property_pk PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.stock_reports (
	id text NOT NULL,
	code text NOT NULL,
	"period" text NOT NULL,
	property_id numeric NOT NULL,
	value numeric NULL,
	last_update timestamp NOT NULL,
	CONSTRAINT stock_reports_pkey PRIMARY KEY (id),
	CONSTRAINT stock_reports_unique UNIQUE (code, period, property_id),
	CONSTRAINT stock_reports_property_id_fk FOREIGN KEY (property_id) REFERENCES public.stock_report_property(id),
	CONSTRAINT stock_reports_stocks_fk FOREIGN KEY (code) REFERENCES public.stocks(code)
);

CREATE TABLE IF NOT EXISTS public.stock_daily (
	id text DEFAULT gen_random_uuid() NOT NULL,
	code text NOT NULL,
	opening_price int4 NULL,
	closing_price int4 NULL,
	high_price int4 NULL,
	low_price int4 NULL,
	volume int8 NULL,
	market_cap int8 NULL,
	"date" date NOT NULL,
	created_at date NULL,
	CONSTRAINT stock_daily_pkey PRIMARY KEY (id),
	CONSTRAINT stock_daily_unique UNIQUE (code, date),
	CONSTRAINT stock_daily_stocks_fk FOREIGN KEY (code) REFERENCES public.stocks(code)
);
--CREATE INDEX stock_daily_date_idx ON public.stock_daily USING btree (date);

CREATE TABLE IF NOT EXISTS public.funds (
	id int2 NOT NULL,
	"name" varchar NOT NULL,
	"type" numeric NULL,
	active bool NULL,
	sharia bool NULL,
	CONSTRAINT funds_pk PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.fund_daily (
	id varchar DEFAULT gen_random_uuid() NOT NULL,
	fund_id int2 NOT NULL,
	value numeric NULL,
	daily_return numeric NULL,
	"date" date NOT NULL,
	CONSTRAINT fund_daily_pk PRIMARY KEY (id),
	CONSTRAINT fund_daily_unique UNIQUE (fund_id, date),
	CONSTRAINT fund_daily_funds_fk FOREIGN KEY (fund_id) REFERENCES public.funds(id)
);
--CREATE INDEX fund_daily_date_idx ON public.fund_daily USING btree (date);

CREATE TABLE IF NOT EXISTS public.fund_aum (
	id text NOT NULL,
	fund_id int2 NOT NULL,
	value numeric NULL,
	"date" date NOT NULL,
	CONSTRAINT fund_nav_pk PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.fund_unit (
	id text NOT NULL,
	fund_id int2 NOT NULL,
	value numeric NULL,
	"date" date NOT NULL,
	CONSTRAINT fund_unit_pk PRIMARY KEY (id)
);